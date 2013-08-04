package org.gridkit.zerormi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.util.concurrent.SimpleTaskService;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.zerormi.RmiPipelineTest.TestIOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RmiSocketConnectionTest {

//	private static final int TEST_TIMEOUT = Integer.MAX_VALUE;
	private static final int TEST_TIMEOUT = 1000;		
	
	private TaskService taskService;
	private ExecutorService executor;
	
	private Socket sockA;
	private Socket sockB;
	
	private RmiGateway sideA;
	private RmiGateway sideB;
	
	@Before
	public void initConnection() throws IOException, InterruptedException, ExecutionException {
		taskService = new SensibleTaskService("executor");
		executor = Executors.newCachedThreadPool();
		int n = 0;
		while(true) {
			if (n++ > 10) {
				throw new RuntimeException("Cannot bind server socket");
			}
			
			int port = 50000 + new Random().nextInt(1000);
			final InetSocketAddress endpoint = new InetSocketAddress("127.0.0.1", port);

			ServerSocket ssock = new ServerSocket();
			try {
				ssock.bind(endpoint);
			}
			catch(IOException e) {
				continue;
			}
			
			Future<Socket> client = executor.submit(new Callable<Socket>() {
				@Override
				public Socket call() throws IOException {
					Socket cl = new Socket();
					cl.connect(endpoint);
					return cl;
				}
			});
			
			sockA = ssock.accept();
			sockB = client.get();
			
			break;
		}

		ByteStream.Duplex duplexA = Streams.toDuplex(sockA, taskService);
		ByteStream.Duplex duplexB = Streams.toDuplex(sockB, taskService);
		
		ClassProvider cp = new SimpleClassProvider(getClass().getClassLoader());
		
		sideA = RmiFactory.createEndPoint("sideA", cp, duplexA, executor, new SmartRmiMarshaler());
		
		sideB = RmiFactory.createEndPoint("sideB", cp, duplexB, executor, new SmartRmiMarshaler());
	}
	
	@After
	public void stopAll() throws InterruptedException {
		if (sockA != null) {
			try {
				sockA.shutdownInput();
				sockA.shutdownOutput();
				sockA.close();
			} catch (IOException e) {
				// ignore
			}
		}
		if (sockB != null) {
			try {
				sockB.shutdownInput();
				sockB.shutdownOutput();
				sockB.close();
			} catch (IOException e) {
				// ignore
			}
		}
		if (sideA != null) {
			sideA.shutdown();
		}
		if (sideB != null) {
			sideB.shutdown();
		}
		if (executor != null) {
			executor.shutdown();
		}
		Thread.sleep(10);
		if (taskService != null) {
			((TaskService.Component)taskService).shutdown();
		}
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void verify_hello_world() throws InterruptedException, ExecutionException {
		"".length();
		FutureEx<Void> ackA = sideA.asExecutor().submit(new Runnable() {			
			@Override
			public void run() {
				System.out.println("Hello A");
				
			}
		});
		FutureEx<Void> ackB = sideB.asExecutor().submit(new Runnable() {			
			@Override
			public void run() {
				System.out.println("Hello B");
				
			}
		});
		ackA.get();
		ackB.get();
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void verify_simple_ping_call() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		final Ping proxy = sideA.asExecutor().submit(new Callable<Ping>() {
			@Override
			public Ping call() throws Exception {
				return new PingServer("A");
			}			
		}).get();

		Assert.assertEquals("A", proxy.ping());
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_simple_echo_call() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		final Echo proxy = sideA.asExecutor().submit(new Callable<Echo>() {
			@Override
			public Echo call() throws Exception {
				return new EchoServer();
			}
		}).get();
		
		Assert.assertEquals("Yaahoo", proxy.echo("Yaahoo"));
	}	
	
	
	private interface Ping extends Remote {
		public String ping() throws TestIOException;
	}
	
	private interface Echo extends Remote {		
		public Object echo(Object obj) throws TestIOException;
	}
	
	public class PingServer implements Ping {
		
		private String replay;

		public PingServer(String replay) {
			this.replay = replay;
		}

		@Override
		public String ping() {
			return replay;
		}
	}	

	public class FaultyServer implements Ping {
		
		private Exception error;
		
		public FaultyServer(Exception error) {
			this.error = error;
		}
		
		@Override
		public String ping() {
			throwUncheked(error);
			throw new Error("Unreachable");
		}
	}	
	
	public static class EchoServer implements Echo {
		@Override
		public Object echo(Object obj) throws TestIOException {
			return obj;
		}		
	}
	
	public static void throwUncheked(Throwable e) {
		class AnyThrow {
		    @SuppressWarnings("unchecked")
		    private <E extends Throwable> void throwAny(Throwable e) throws E {
		        throw (E)e;
		    }
		};
		new AnyThrow().<RuntimeException>throwAny(e);
	}
}
