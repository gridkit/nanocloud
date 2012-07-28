package org.gridkit.util.vicontrol.isolate.coherence;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.gridkit.util.vicontrol.ViConfigurable;
import org.gridkit.util.vicontrol.ViNode;
import org.gridkit.util.vicontrol.isolate.Isolate;
import org.gridkit.util.vicontrol.isolate.IsolateViNode;

import com.tangosol.coherence.component.net.extend.RemoteService;
import com.tangosol.coherence.component.net.extend.connection.TcpConnection;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.Service;
import com.tangosol.net.management.MBeanServerFinder;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

public class CohHelper {

	public static void pofConfig(ViConfigurable node, String path) {
		node.setProp("tangosol.pof.config", path);
	}

	public static void cacheConfig(ViConfigurable node, String path) {
		node.setProp("tangosol.coherence.cacheconfig", path);
	}

	public static void localstorage(ViConfigurable node, boolean enabled) {
		node.setProp("tangosol.coherence.distributed.localstorage", String.valueOf(enabled));
	}

	public static void enableFastLocalCluster(ViConfigurable node) {
		int port = new Random().nextInt(10000) + 50000;
		node.setProp("tangosol.coherence.ttl", "0");
		node.setProp("tangosol.coherence.wka", "127.0.0.1");
		node.setProp("tangosol.coherence.wka.port", String.valueOf(port));
		node.setProp("tangosol.coherence.localhost", "127.0.0.1");
		node.setProp("tangosol.coherence.localport", String.valueOf(port));
		node.setProp("tangosol.coherence.socketprovider", "tcp");
		node.setProp("tangosol.coherence.cluster", "jvm::" + ManagementFactory.getRuntimeMXBean().getName());
		setJoinTimeout(node, 100);
		disableTcpRing(node);
		enableShutdownOnExit(node);
	}

	public static void enableShutdownOnExit(ViConfigurable node) {
		node.addShutdownHook("com.oracle.coherence.shutdown-on-stop", new Runnable() {
			@Override
			public void run() {
				Cluster cluster = CacheFactory.getCluster();
				if (cluster.isRunning()) {
					cluster.stop();
				}
				CacheFactory.shutdown();
			}
		}, true);
	}

	/**
	 * Starts Coherence same way as {@link DefaultCacheServer#main(String[])} will do.
	 * @param node
	 */
	public static void startDefaultCacheServer(ViNode node) {
		node.exec(new Runnable() {
			@Override
			public void run() {
				// this will invoke DefaultCacheServer startup logic
				DefaultCacheServer.start();
				// cluster should be started at this point
			}
		});
	}
	
	public static void disableTCMP(ViConfigurable node) {
		node.setProp("tangosol.coherence.tcmp.enabled", "false");
	}

	public static void enableJmx(ViConfigurable node) {
		node.setProp("tangosol.coherence.management", "local-only");
		node.setProp("tangosol.coherence.management.jvm.all", "false");
		node.setProp("tangosol.coherence.management.remote", "false");
		node.setProp("tangosol.coherence.management.serverfactory", IsolateMBeanFinder.class.getName());
	}
	
	/**
	 * Coherence does not have standard property for cluster join timeout.
	 * This method will patch op. configuration directly.
	 */
	public static void setJoinTimeout(ViConfigurable node, final long timeout) {
		Runnable action = new Runnable() {
			@Override
			public void run() {
				Cluster cluster = CacheFactory.getCluster();
				if (cluster.isRunning()) {
					throw new IllegalStateException("Cluster is already started");
				}
				XmlElement config = CacheFactory.getClusterConfig();
				XmlHelper.ensureElement(config, "cluster-config/multicast-listener/join-timeout-milliseconds")
					.setLong(timeout);
				CacheFactory.setServiceConfig("Cluster", config);
			}
		};
		node.addStartupHook("com.oracle.coherence.init-join-timeout", action, true);
	}

	/**
	 * Coherence does not have standard property to disable TCP ring.
	 * TCP ring tends to hung in isolate environment, so disabling it makes testing more stable.
	 * This method will patch op. configuration directly.
	 */
	public static void disableTcpRing(ViConfigurable node) {
		Runnable action = new Runnable() {
			@Override
			public void run() {
				Cluster cluster = CacheFactory.getCluster();
				if (cluster.isRunning()) {
					throw new IllegalStateException("Cluster is already started");
				}
				XmlElement config = CacheFactory.getClusterConfig();
				XmlHelper.ensureElement(config, "cluster-config/tcp-ring-listener/enabled")
				.setBoolean(false);
				CacheFactory.setServiceConfig("Cluster", config);
			}
		};
		
		node.addStartupHook("com.oracle.coherence.disable-tcp-ring", action, true);
	}

	/**
	 * Closes TCP connection for given remote service.
	 * Useful for Extend fail over testing.
	 */
	public static void killTcpInitiator(String serviceName) {
		for(Service s: getLocalServices()) {
			if (serviceName.equals(s.getInfo().getServiceName())) {
				killTcpInitiator(s);
			}
		}
	}
	
	/**
	 * Closes TCP connections for all remote services.
	 * Useful for Extend fail over testing.
	 */
	public static void killTcpAllInitiators() {
		for(Service s: getLocalServices()) {
			if (s instanceof RemoteService) {
				killTcpInitiator(s);
			}
		}
	}
	
	private static void killTcpInitiator(Service s) {
		RemoteService rs = (RemoteService) s;
		try {
			TcpConnection tcp = (TcpConnection) rs.getInitiator().ensureConnection();
			Socket sock = tcp.getSocket();
			System.err.println("Dropping Extend socket " + rs.getInfo().getServiceName() + " | " + sock);
			sock.close();
		} catch (IOException e) {
			// ignore
		}
	}

	@SuppressWarnings("unchecked")
	private static Set<Service> getLocalServices() {
		try {
			Cluster cluster = CacheFactory.getCluster();
			Method m = SafeCluster.class.getDeclaredMethod("getLocalServices");
			m.setAccessible(true);
			return (Set<Service>) m.invoke(cluster);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Object jmxAttribute(ViNode node, ObjectName name, String attribute) {
		try {
			MBeanServer mserver = getMBeanServer(node);
			Object bi = mserver.getAttribute(name, attribute);
			if (bi == null) {
				return 0;
			}
			else {
				return bi;
			}
		}
		catch (InstanceNotFoundException e) {
			return null;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	private static <V> boolean waitFor(Callable<V> condition, V excepted, long timeoutMs) {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		do {
			V val;
			try {
				val = condition.call();
			} catch (Exception e) {
				continue;
			}
			if (val == null) {
				if (excepted == null) {
					return true;
				}
			}
			else {
				if (val.equals(excepted)) {
					return true;
				}
			}
			try {
				Thread.sleep(100);
			}
			catch(InterruptedException e) {				
			}
		}
		while(deadline > System.nanoTime());
			
		return false;
	}
	
	private static ObjectName mbeanCluster() {
		try {
			return new ObjectName("Coherence:type=Cluster");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static ObjectName mbeanServiceName(String name,int nodeId) {
		try {
			return new ObjectName("Coherence:type=Service,name=" + name + ",nodeId=" + nodeId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static int jmxMemberId(ViNode node) {
		Object x = jmxAttribute(node, mbeanCluster(), "LocalMemberId");
		return x == null ? 0 : ((Integer)x).intValue();
	}

	public static void jmxCloseProxyConnections(ViNode node) {
		jmxCloseProxyConnections(node, "*");
	}

	public static void jmxCloseProxyConnections(ViNode node, String proxyServiceName) {		
		final MBeanServer server = getMBeanServer(node);
		int id = jmxMemberId(node);
		for(final ObjectName name : server.queryNames(null, null)) {
			if (isConnectionBean(name) && String.valueOf(id).equals(name.getKeyProperty("nodeId"))) {
				if (!"*".equals(proxyServiceName)) {
					if (!proxyServiceName.equals(name.getKeyProperty("name"))) {
						continue;
					}
				}
				// Separate thread is required if we are executing on that connection
				Thread thread = new Thread() {
					@Override
					public void run() {
						try {
							server.invoke(name, "closeConnection", new Object[0], new String[0]);
							System.err.println("Extend conntection closed: " + name);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	public static Collection<ObjectName> jmxListProxyConnections(ViNode node) {
		return jmxListProxyConnections(node, "*");
	}

	public static Collection<ObjectName> jmxListProxyConnections(ViNode node, String proxyServiceName) {		
		final MBeanServer server = getMBeanServer(node);
		int id = jmxMemberId(node);
		List<ObjectName> result = new ArrayList<ObjectName>();
		for(final ObjectName name : server.queryNames(null, null)) {
			if (isConnectionBean(name) && String.valueOf(id).equals(name.getKeyProperty("nodeId"))) {
				if (!"*".equals(proxyServiceName)) {
					if (!proxyServiceName.equals(name.getKeyProperty("name"))) {
						continue;
					}
				}
				result.add(name);
			}
		}
		return result;
	}
	
	private static boolean isConnectionBean(ObjectName name) {
		return "Coherence".equals(name.getDomain())
				&& "Connection".equals(name.getKeyProperty("type"));
	}

	public static String jmxServiceStatusHA(ViNode node, String serviceName) {
		String s = (String) jmxAttribute(node, mbeanServiceName(serviceName, jmxMemberId(node)), "StatusHA");
		return s;
	}

	public static boolean jmxServiceRunning(ViNode node, String serviceName) {
		Boolean b = (Boolean) jmxAttribute(node, mbeanServiceName(serviceName, jmxMemberId(node)), "Running");
		return b == null ?  false : b.booleanValue();
	}

	public static void jmxWaitForService(ViNode node, String serviceName) {
		jmxWaitForService(node, serviceName, 30000); // Coherence is slow, 30000
	}

	public static void jmxWaitForService(final ViNode node, final String serviceName, long timeoutMs) {
		if (!waitFor(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return jmxServiceRunning(node, serviceName);
			}
			
		}, Boolean.TRUE, timeoutMs)) {
			throw new AssertionError("Wait for service have failed");
		}
	}

	public static void jmxWaitForStatusHA(ViNode node, String serviceName, String status) {
		jmxWaitForStatusHA(node, serviceName, status, 30000); // Coherence is slow, 30000		
	}

	public static void jmxWaitForStatusHA(final ViNode node, final String serviceName, final String status, long timeoutMs) {
		if (!waitFor(new Callable<String>() {
			
			@Override
			public String call() throws Exception {
				return jmxServiceStatusHA(node, serviceName);
			}
			
		}, status, timeoutMs)) {
			throw new AssertionError("Wait for service have failed");
		}
	}

	public static void jmxDumpMBeans(ViNode node) {
		MBeanServer server = getMBeanServer(node);
		for(ObjectName name : server.queryNames(null, null)) {
			System.out.println(name);
		}
	}
	
	private static MBeanServer getMBeanServer(ViNode node) {
		if (node != null) {
			// cluster connection my take sometime
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(3500);
			MBeanServer server = null;
			while(deadline > System.nanoTime()) {			
				server = (MBeanServer) ((IsolateViNode)node).getIsolate().getGlobal(CohHelper.class, "MBeanServer");
				if (server == null) {
					LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
				}
				else {
					break;
				}			
			}
			if (server == null) {
				throw new IllegalStateException("Local JMX is not enabled for node " + node.toString());
			}
			return server;
		}
		else {
			MBeanServer server = IsolateMBeanFinder.MSERVER;
			if (server == null) {
				throw new IllegalStateException("Local JMX is not enabled for this node");
			}
			return server;
		}
	}
	
	public static class IsolateMBeanFinder implements MBeanServerFinder {

		static MBeanServer MSERVER = null;
		
		@Override
		public synchronized MBeanServer findMBeanServer(String sDefaultDomain) {
			if (MSERVER == null) {
				MSERVER = MBeanServerFactory.newMBeanServer(System.getProperty("isolate.name"));
				Isolate.currentIsolate().setGlobal(CohHelper.class, "MBeanServer", MSERVER);
			}
			return MSERVER;
		}
	}
}
