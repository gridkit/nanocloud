package org.gridkit.zerormi;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.zerormi.hub.DirectConnectSocket;
import org.gridkit.zerormi.hub.RemotingEndPoint;
import org.gridkit.zerormi.hub.RemotingHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.gridkit.zerormi.hub.SimpleSocketAcceptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class RemotingHubTest {

	private RemotingHub hub;
	private RemotingEndPoint endPoint1;
	private RemotingEndPoint endPoint2;
	private SimpleSocketAcceptor acceptor;
	private RmiGateway gateway1;
	private RmiGateway gateway2;
	private AdvancedExecutor remoteExecutor1;
	private AdvancedExecutor remoteExecutor2;
	
	@Before
	public void initHub() throws InterruptedException, BrokenBarrierException, TimeoutException {
		hub = new RemotingHub();
		
		final CountDownLatch latch = new CountDownLatch(2);
		
		SessionEventListener sessionListener = new SessionEventListener() {
			@Override
			public void reconnected(DuplexStream stream) {
				System.out.println("HUB: reconnected: " + stream);
			}
			
			@Override
			public void interrupted(DuplexStream stream) {
				System.out.println("HUB: interrupted: " + stream);
			}
			
			@Override
			public void connected(DuplexStream stream) {
				System.out.println("HUB: connected: " + stream);
				latch.countDown();
			}
			
			@Override
			public void closed() {
				System.out.println("HUB: closed");
			}
		};
		
		String uid1 = hub.newSession("side1", sessionListener);
		String uid2 = hub.newSession("side2", sessionListener);
		
		
		acceptor = new SimpleSocketAcceptor();
		ServerSocket ssock;
		try {
			ssock = new ServerSocket(21000);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		acceptor.bind(ssock, hub);
		acceptor.start();
		
		endPoint1 = new RemotingEndPoint("slave1", uid1, new DirectConnectSocket("localhost", 21000));
		new Thread(endPoint1).start();
		
		gateway1 = hub.getGateway(uid1);
		remoteExecutor1 = gateway1.asExecutor();

		endPoint2 = new RemotingEndPoint("slave2", uid2, new DirectConnectSocket("localhost", 21000));
		new Thread(endPoint2).start();
		
		gateway2 = hub.getGateway(uid2);
		remoteExecutor2 = gateway2.asExecutor();
		
		latch.await(5000, TimeUnit.MILLISECONDS);
	}
	
	@After
	public void shutdown() {
		gateway1.shutdown();
		gateway2.shutdown();
		hub.closeAllConnections();
		acceptor.close();
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void verify_executor1() throws InterruptedException, ExecutionException {
		Assert.assertEquals("abc", remoteExecutor1.submit(new Echo("abc")).get());
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void verify_executor2() throws InterruptedException, ExecutionException {
		Assert.assertEquals("abc", remoteExecutor2.submit(new Echo("abc")).get());
	}
	
	@Test
	public void transitive_proxy_test() throws InterruptedException, ExecutionException {
		Future<Callable<String>> future = remoteExecutor1.submit(new RemoteProxyMaker<String>(new Echo<String>("123")));
		String result = remoteExecutor2.submit(future.get()).get();
		Assert.assertEquals("123", result);		
	}
	
	@Test @Ignore
	public void test_resource_leak() throws InterruptedException, ExecutionException {

		SessionEventListener listener = new SessionEventLogger();		
		
		Random rnd = new Random(1);
		Map<String, RemotingEndPoint> slaves = new HashMap<String, RemotingEndPoint>(); 
		List<String> sessions = new ArrayList<String>();
		for(int i = 0; i != 10000; ++i) {
			if (sessions.size() < 10) {
				String id = hub.newSession("slave-" + i, listener);
				RemotingEndPoint slave = createSlave("slave-" + i, id);
				sessions.add(id);
				slaves.put(id, slave);
				
				hub.getGateway(id).asExecutor().submit(new Callable<String>() {
					@Override
					public String call() throws Exception {
						return "Ping";
					}					
				}).get();
			}
			int n = rnd.nextInt(sessions.size());
			hub.getGateway(sessions.get(n)).asExecutor().submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					return "Ping";
				}					
			}).get();
			if (rnd.nextInt(100) < 30) {
				String id = sessions.remove(n);
				slaves.remove(id).shutdown();
				hub.closeConnection(id);
			}
		}
	}
	
	private RemotingEndPoint createSlave(String name, String id) {
		RemotingEndPoint endPoint = new RemotingEndPoint(name, id, new DirectConnectSocket("localhost", 21000));
		new Thread(endPoint).start();
		return endPoint;
	}

	private final class SessionEventLogger implements
			SessionEventListener {
		@Override
		public void reconnected(DuplexStream stream) {
			System.out.println("HUB: reconnected: " + stream);
		}

		@Override
		public void interrupted(DuplexStream stream) {
			System.out.println("HUB: interrupted: " + stream);
		}

		@Override
		public void connected(DuplexStream stream) {
			System.out.println("HUB: connected: " + stream);
		}

		@Override
		public void closed() {
			System.out.println("HUB: closed");
		}
	}

	@SuppressWarnings("serial")
	public static class Echo<V> implements Callable<V>, Serializable {

		private V sound;
		
		public Echo(V sound) {
			this.sound = sound;
		}

		@Override
		public V call() throws Exception {
			return sound;
		}
	}

	@SuppressWarnings("serial")
	public static class SelfIdentity implements Callable<SelfIdentity>, Serializable {
		
		public SelfIdentity() {
		}
		
		@Override
		public SelfIdentity call() throws Exception {
			return this;
		}
	}
	
	public static class NotSerializable implements Callable<String> {
		@Override
		public String call() throws Exception {
			return "NotSerializable";
		}
	}

	@SuppressWarnings("serial")
	public static class SerializableAdapter<V> implements Callable<V>, Serializable {
		
		private Callable<V> callable;
		
		public SerializableAdapter(Callable<V> callable) {
			this.callable = callable;
		}

		@Override
		public V call() throws Exception {
			return callable.call();
		}
	}
	
	public static interface ProxyCallable<V> extends Callable<V>, Remote {
		
	}
	
	public static class ProxyAdapter<V> implements ProxyCallable<V> {
		
		private Callable<V> callable;
		
		public ProxyAdapter(Callable<V> callable) {
			this.callable = callable;
		}

		@Override
		public V call() throws Exception {
			return callable.call();
		}
	}	
	
	@SuppressWarnings("serial")
	public static class RemoteProxyMaker<V> implements Callable<Callable<V>>, Serializable {
		
		private Callable<V> nested;

		public RemoteProxyMaker(Callable<V> nested) {
			this.nested = nested;
		}

		@Override
		public Callable<V> call() throws Exception {
			return new ProxyAdapter<V>(nested);
		}
	}
}
