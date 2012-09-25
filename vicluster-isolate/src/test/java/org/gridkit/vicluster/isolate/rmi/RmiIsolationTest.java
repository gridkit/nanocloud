package org.gridkit.vicluster.isolate.rmi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.gridkit.vicluster.ViCloud;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.gridkit.vicluster.spi.IsolateFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RmiIsolationTest {

	ViCloud<IsolateViNode>  cloud;
	
	@Before 
	public void initCloud() {
		cloud = IsolateFactory.createIsolateCloud();
		cloud.byName("**").isolation().includePackage("org.gridkit");
	}
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	/**
	 * Verifies that RMI will use correct context class loader for deserializing requests.
	 */
	@Test
	public void verify_isolate_connectivity() {
		
		IsolateViNode node1 = cloud.node("node1");
		IsolateViNode node2 = cloud.node("node2");
		
		IsolateViNode nodes = cloud.node("**");
		nodes.isolation().includePackage("org.gridkit");
		
		node1.exec(new VoidCallable() {
			@Override
			public void call() throws Exception {
				
				PingServer server = new PingServer();
				Remote serverStub = UnicastRemoteObject.exportObject(server, 0);
				
				Registry reg = LocateRegistry.createRegistry(10000);
				reg.bind("ping", serverStub);
				
				reg = LocateRegistry.getRegistry(10000);
				
				RemotePing rping = (RemotePing) reg.lookup("ping");
				
				PingObject ping = new PingObject("pong");
				System.out.println("Ping: " + ping.getClass().getClassLoader());
				assertThat(rping.ping(ping), is("pong"));
			}
		});

		node2.exec(new VoidCallable() {
			@Override
			public void call() throws Exception {
				
				Registry reg = LocateRegistry.getRegistry(10000);
				
				RemotePing rping = (RemotePing) reg.lookup("ping");
				
				PingObject ping = new PingObject("pong");
				System.out.println("Ping: " + ping.getClass().getClassLoader());
				assertThat(rping.ping(ping), is("pong"));
			}
		});
		
	}
	
	
	public static interface RemotePing extends Remote {
		
		public String ping(PingObject ping) throws RemoteException;
	}
	
	public static class PingServer implements RemotePing {
		@Override
		public String ping(PingObject ping) throws RemoteException {
			System.out.println("Pong: " + ping.getClass().getClassLoader());
			return ping.pong;
		}
	}
	
	@SuppressWarnings("serial")
	public static class PingObject implements Serializable {
		
		public String pong;

		public PingObject(String pong) {
			this.pong = pong;
		}
	}
}
