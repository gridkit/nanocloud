package org.gridkit.zerormi;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.gridkit.bjtest.BetterParameterized;
import org.gridkit.bjtest.BetterParameterized.Parameters;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.zerormi.ByteStream.Duplex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BetterParameterized.class)
public class RmiPipelineTest {

//	private static final int TEST_TIMEOUT = Integer.MAX_VALUE;
	private static final int TEST_TIMEOUT = 1000;	
	

//	@BeforeClass
	public static void enableTrace() {
		ReliableBlobPipe.TRACE = true;
		SmartObjectSerializer.TRACE = true;
	}

	@AfterClass
	public static void disableTrace() {
		ReliableBlobPipe.TRACE = false;
		SmartObjectSerializer.TRACE = false;
	}
	
	@Parameters
	public static List<Object[]> parameters() {
		List<Object[]> flavours = new ArrayList<Object[]>();
		flavours.add(new Object[]{"sync", 4 << 10});
		flavours.add(new Object[]{"async", 4 << 10});
		flavours.add(new Object[]{"async", 64});
		flavours.add(new Object[]{"async", 256});
		return flavours;
	}
	
	private ByteStream.SyncBytePipe syncSocketA;
	private ByteStream.SyncBytePipe syncSocketB;
	
	private PumpedWriteAdapter pumpA;
	private PumpedWriteAdapter pumpB;
	
	private ReliableBlobPipe pipeA;
	private ReliableBlobPipe pipeB;
	
	private SmartObjectSerializer objPipeA;
	private SmartObjectSerializer objPipeB;
	
	private RmiChannel2 sideA;
	private RmiChannel2 sideB;
	
	@Before
	public void initSocket() {
		syncSocketA = new ByteStream.SyncBytePipe();
		syncSocketB = new ByteStream.SyncBytePipe();
		
		syncSocketA.bind(syncSocketB);

		pipeA = new ReliableBlobPipe("sideA", new Superviser());
		pipeB = new ReliableBlobPipe("sideB", new Superviser());

		pumpA = new PumpedWriteAdapter(4 << 10, syncSocketA);
		pumpB = new PumpedWriteAdapter(4 << 10, syncSocketB);

		ClassProvider cp = new SimpleClassProvider(RmiPipelineTest.class.getClassLoader());		
		
		Executor directExecutor = new Executor() {			
			@Override
			public void execute(Runnable command) {
				command.run();
			}
			
			@Override
			public String toString() {
				return "directExecutor";
			}
		};
		
		sideA = new RmiChannel2("sideA", new Superviser(), cp, directExecutor);

		sideB = new RmiChannel2("sideB", new Superviser(), cp, Executors.newCachedThreadPool());
		
		RmiMarshalStack rmsA = new RmiMarshalStack(new SmartRmiMarshaler(), sideA);
		RmiMarshalStack rmsB = new RmiMarshalStack(new SmartRmiMarshaler(), sideB);
		
		objPipeA = new SmartObjectSerializer(pipeA, rmsA, cp);
		objPipeB = new SmartObjectSerializer(pipeB, rmsB, cp);
		
		sideA.setPipe(objPipeA);
		sideB.setPipe(objPipeB);
		
		pipeA.setStream(pumpA);
		pipeB.setStream(pumpB);

// 		Pumping SYNCs before test, prevents sending first message twice		
//		pumpA.pump();
//		pumpB.pump();		
	}

	private void pump() throws InterruptedException {
		while(true) {
			boolean hasA = pumpA.hasPending();
			boolean hasB = pumpB.hasPending();
//			System.out.println("pump /" + hasA + "/" + hasB);
			if (hasA || hasB) {
				pumpA.pump(pumpBlock);
				pumpB.pump(pumpBlock);
				continue;
			}
			else {
				break;
			}
		}	
	}
	
	private boolean syncCall;
	private int pumpBlock = 4 << 10;
	
	public RmiPipelineTest(String callMethod, int pumpBlock) {
		this.syncCall = "sync".equals(callMethod);
		this.pumpBlock = pumpBlock;
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_simple_ping_call() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		PingServer serverA = new PingServer("A");
		final Ping proxy = exportAtoB(serverA);

		Future<String> pingResult = futurePing(proxy);

		pump();
		
		Assert.assertEquals("A", pingResult.get());
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_simple_echo_call() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		EchoServer serverA = new EchoServer();
		final Echo proxy = exportAtoB(serverA);
		
		Future<Object> pingResult = futureEcho(proxy, "Yaahoo");
		
		pump();
		
		Assert.assertEquals("Yaahoo", pingResult.get());
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_exception_passing() throws IOException, InterruptedException, SecurityException, ExecutionException, NoSuchMethodException {
		NullPointerException e1 = new NullPointerException("HA-HA");
		Ping serverA = new FaultyServer(e1);
		Ping proxy = exportAtoB(serverA);
		
		Future<String> call = futurePing(proxy);
		
		pump();
		
		try {
			call.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals(e1.toString(), e.getCause().toString());
		}
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void verify_first_write_failure_handling() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		Echo serverA = new EchoServer();
		final Echo proxy = exportAtoB(serverA);

		Future<Object> call = futureEcho(proxy, new FailOnFirstWrite());

		pump();
		
		assert_exception(call, new NotSerializableException("FailOnFirstWrite"));
		
		verify_simple_echo_call();
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_first_read_failure_handling() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		Echo serverA = new EchoServer();
		final Echo proxy = exportAtoB(serverA);
		
		Future<Object> call = futureEcho(proxy, new FailOnFirstRead());
		
		pump();
		
		assert_exception(call, new IOException("Remote error: java.io.NotSerializableException: FailOnFirstRead"));
		
		verify_simple_echo_call();
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_second_write_failure_handling() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		Echo serverA = new EchoServer();
		final Echo proxy = exportAtoB(serverA);

		Future<Object> call = futureEcho(proxy, new FailOnSecondWrite());

		pump();
		
		assert_exception(call, new NotSerializableException("FailOnSecondWrite"));
		
		verify_simple_echo_call();
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_second_read_failure_handling() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		Echo serverA = new EchoServer();
		final Echo proxy = exportAtoB(serverA);
		
		Future<Object> call = futureEcho(proxy, new FailOnSecondRead());
		
		pump();
		
		assert_exception(call, new IOException("Failed to marshal call results: java.io.IOException: Remote error: java.io.NotSerializableException: FailOnSecondRead"));
		
		verify_simple_echo_call();
	}	
	
	private void assert_exception(Future<Object> call, Throwable expected) throws InterruptedException {
		try {
			call.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Throwable error = e.getCause();
			if (error instanceof UndeclaredThrowableException) {
				error = ((UndeclaredThrowableException)error).getUndeclaredThrowable();
			}
			Assert.assertEquals(error.toString(), expected.toString());
		}		
	}

	private Future<String> futurePing(final Ping proxy) throws InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		if (syncCall) {
			return asyncSyncPing(proxy);
		}
		else {
			return asyncAsyncPing(proxy);
		}
	}

	private Future<Object> futureEcho(Echo proxy, Object arg) throws InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		if (syncCall) {
			return asyncSyncEcho(proxy, arg);
		}
		else {
			return asyncAsyncEcho(proxy, arg);
		}
	}
	
	private Future<String> asyncSyncPing(final Ping proxy) throws InterruptedException, ExecutionException {
		final FutureBox<Void> start = new FutureBox<Void>();
		FutureTask<String> pingFuture = new FutureTask<String>( new Callable<String>() {
			@Override
			public String call() throws Exception {
				start.setData(null);
				return proxy.ping();
			}
		});
		Thread t = new Thread(pingFuture);
		t.start();
		start.get();
		Thread.sleep(50);
		return pingFuture;
	}	

	private Future<Object> asyncSyncEcho(final Echo proxy, final Object arg) throws InterruptedException, ExecutionException {
		final FutureBox<Void> start = new FutureBox<Void>();
		FutureTask<Object> echoFuture = new FutureTask<Object>( new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				start.setData(null);
				return proxy.echo(arg);
			}
		});
		Thread t = new Thread(echoFuture);
		t.start();
		start.get();
		Thread.sleep(50);
		return echoFuture;
	}	

	private Future<String> asyncAsyncPing(final Ping proxy) throws SecurityException, NoSuchMethodException {
		RmiInvocationHandler rt = RemoteStub.getRmiChannel(proxy);
		Future<String> result = rt.invokeRemotely(proxy, Ping.class.getMethod("ping"));
		return result;
	}

	private Future<Object> asyncAsyncEcho(final Echo proxy, Object arg) throws SecurityException, NoSuchMethodException {
		RmiInvocationHandler rt = RemoteStub.getRmiChannel(proxy);
		Future<Object> result = rt.invokeRemotely(proxy, Echo.class.getMethod("echo", Object.class), arg);
		return result;
	}
	
	
	private Ping exportAtoB(Ping serverA) throws IOException {
		sideA.exportObject(Ping.class, serverA);
		Object rmiRef = sideA.writeReplace(serverA);
		final Ping proxy = (Ping) sideB.readResolve(rmiRef);
		return proxy;
	}

	private Echo exportAtoB(Echo serverA) throws IOException {
		sideA.exportObject(Echo.class, serverA);
		Object rmiRef = sideA.writeReplace(serverA);
		final Echo proxy = (Echo) sideB.readResolve(rmiRef);
		return proxy;
	}

	private interface Ping {
		public String ping() throws TestIOException;
	}
	
	private interface Echo {		
		public Object echo(Object obj) throws TestIOException;
	}
	
	@SuppressWarnings("serial")
	public static class TestIOException extends IOException {

		public TestIOException() {
			super();
		}

		public TestIOException(String message, Throwable cause) {
			super(message, cause);
		}

		public TestIOException(String message) {
			super(message);
		}

		public TestIOException(Throwable cause) {
			super(cause);
		}		
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
	
	public static class FailOnFirstWrite implements Serializable {
		
		private Object writeReplace() throws ObjectStreamException {
			throw new NotSerializableException("FailOnFirstWrite");
		}		
		
		private Object readResolve() throws ObjectStreamException {
			return this;
		}		
	}

	public static class FailOnFirstRead implements Serializable {
		
		private Object writeReplace() throws ObjectStreamException {
			return this;
		}		
		
		private Object readResolve() throws ObjectStreamException {
			throw new NotSerializableException("FailOnFirstRead");
		}		
	}

	public static class FailOnSecondWrite implements Serializable {
		
		boolean deserialized = false;
		
		private Object writeReplace() throws ObjectStreamException {
			throw new NotSerializableException("FailOnSecondWrite");
		}		
		
		private Object readResolve() throws ObjectStreamException {
			if (deserialized) {
				return this;
			}
			else {
				deserialized = true;
				return this;
			}
		}		
	}

	public static class FailOnSecondRead implements Serializable {
		
		boolean deserialized = false;
		
		private Object writeReplace() throws ObjectStreamException {
			return this;
		}		
		
		private Object readResolve() throws ObjectStreamException {
			if (deserialized) {
				throw new NotSerializableException("FailOnSecondRead");
			}
			else {
				deserialized = true;
				return this;
			}
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
	
	private static class Superviser implements ReliableBlobPipe.PipeSuperviser {

		@Override
		public void onWarning(SuperviserEvent event) {
			System.err.println(event);
		}

		@Override
		public void onTermination(SuperviserEvent event) {
			System.err.println(event);
		}

		@Override
		public void onFatalError(SuperviserEvent event) {
			System.err.println(event);
		}

		@Override
		public void onStreamRejected(ReliableBlobPipe pipe, Duplex stream,	Exception e) {
			System.out.println("Stream rejected: " + e);
			e.printStackTrace();
		}
	}
}
