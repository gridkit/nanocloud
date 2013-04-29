package org.gridkit.vicluster;

import java.rmi.Remote;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.nanocloud.CloudFactory;
import org.junit.After;
import org.junit.Test;

public class TransparentRemotingTest {

	private ViManager cloud = CloudFactory.createCloud();
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	public ViNode getIsolateNode() {
		ViNode node = cloud.node("isolate");
		ViProps.at(node).setIsolateType();
		return node;
	}
	
	@Test
	public void check_isolate_with_simple_double_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newSimpleServer(node);
		
		ping.ping();
		Assert.assertEquals(0d, ping.pingDouble(10d));
	}

	@Test
	public void check_isolate_with_simple_long_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newSimpleServer(node);
		
		ping.ping();
		Assert.assertEquals(0, ping.pingLong(10l));
	}

	@Test
	public void check_isolate_with_simple_int_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newSimpleServer(node);
		
		ping.ping();
		Assert.assertEquals(0, ping.pingInt(10));
	}

	@Test
	public void check_isolate_with_simple_string_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newSimpleServer(node);
		
		ping.ping();
		Assert.assertEquals(null, ping.pingString("ABC"));
	}

	@Test(timeout = 2000)
	public void check_isolate_with_inverting_double_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newInvertedServer(node);
		
		ping.ping();
		Assert.assertEquals(-10d, ping.pingDouble(10d));
	}

	@Test(timeout = 2000)
	public void check_isolate_with_inverting_long_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newInvertedServer(node);
		
		ping.ping();
		Assert.assertEquals(-10l, ping.pingLong(10l));
	}

	@Test(timeout = 2000)
	public void check_isolate_with_inverting_int_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newInvertedServer(node);
		
		ping.ping();
		Assert.assertEquals(-10, ping.pingInt(10));
	}

	@Test(timeout = 2000)
	public void check_isolate_with_inverting_string_ping() {
		ViNode node = getIsolateNode();
		Ping ping = newInvertedServer(node);
		
		ping.ping();
		Assert.assertEquals("ABC", ping.pingString("ABC"));
	}

	public Ping newInvertedServer(ViNode node) {
		Ping ping = node.exec(new Callable<Ping>() {
			@Override
			public Ping call() throws Exception {				
				return new InveringPingServer();
			}
		});
		return ping;
	}

	public Ping newSimpleServer(ViNode node) {
		Ping ping = node.exec(new Callable<Ping>() {
			@Override
			public Ping call() throws Exception {				
				return new SimplePingServer();
			}
		});
		return ping;
	}
	
	public interface Ping extends Remote {
		
		public void ping();
		
		public double pingDouble(double val);

		public int pingInt(int val);

		public long pingLong(long val);

		public String pingString(String val);
		
	}
	
	public class SimplePingServer implements Ping {

		@Override
		public void ping() {
		}

		@Override
		public double pingDouble(double val) {
			return 0;
		}

		@Override
		public int pingInt(int val) {
			return 0;
		}

		@Override
		public long pingLong(long val) {
			return 0;
		}

		@Override
		public String pingString(String val) {
			return null;
		}
	}
	
	public class InveringPingServer extends SimplePingServer {

		@Override
		public void ping() {
			super.ping();
		}

		@Override
		public double pingDouble(double val) {
			return -val;
		}

		@Override
		public int pingInt(int val) {
			return -val;
		}

		@Override
		public long pingLong(long val) {
			return -val;
		}

		@Override
		public String pingString(String val) {
			return val;
		}
	}
	
}
