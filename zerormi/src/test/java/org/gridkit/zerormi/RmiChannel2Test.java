package org.gridkit.zerormi;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RmiChannel2Test {

	private static final int TEST_TIMEOUT = Integer.MAX_VALUE;
//	private static final int TEST_TIMEOUT = 1000;
	
	@BeforeClass
	public static void enableTrace() {
		RmiChannel2.TRACE = false;
	}
	
	@AfterClass
	public static void disableTrace() {
		RmiChannel2.TRACE = false;
	}
	
	private ExecutorService es;
	private RmiChannel2 sideA;
	private RmiChannel2 sideB;
	private PlugMock plugA;
	private PlugMock plugB;
	
	@Before
	public void init() {
		es = Executors.newCachedThreadPool();
		plugA = new PlugMock("A");
		plugB = new PlugMock("B");
		
		sideA = new RmiChannel2("side-A", new TestSuperviser(), getClassProvider(), es);
		sideB = new RmiChannel2("side-B", new TestSuperviser(), getClassProvider(), es);
		
		plugA.recoder = new ObjectRecoder(sideA, sideB);
		plugB.recoder = new ObjectRecoder(sideB, sideA);
		
		sideA.setPipe(plugA);
		sideB.setPipe(plugB);
	}

	@After	
	public void tearDown() {
		es.shutdownNow();
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void verify_simple_call() throws IOException, InterruptedException, ExecutionException {
		PingServer serverA = new PingServer("A");
		final Ping proxy = exportAtoB(serverA);

		Future<String> pingResult = asyncSyncPing(proxy);

		plugB.waitForMessage();
		
		Assert.assertThat(plugB, has_one_pending_message());
		Assert.assertThat(plugB, has_pending_call(Ping.class, "ping"));
		
		plugB.play(plugA);

		plugA.waitForMessage();
		Assert.assertThat(plugA, has_one_pending_message());
		Assert.assertThat(plugA, has_pending_result("A"));
		
		plugA.play(plugB);
		
		Assert.assertEquals("A", pingResult.get());
	}

	@Test(timeout=TEST_TIMEOUT)
	public void verify_simple_async_call() throws IOException, InterruptedException, ExecutionException, SecurityException, NoSuchMethodException {
		PingServer serverA = new PingServer("A");
		final Ping proxy = exportAtoB(serverA);
		
		Future<String> pingResult = asyncAsyncPing(proxy);
		
		plugB.waitForMessage();
		
		Assert.assertThat(plugB, has_one_pending_message());
		Assert.assertThat(plugB, has_pending_call(Ping.class, "ping"));
		
		plugB.play(plugA);
		
		plugA.waitForMessage();
		Assert.assertThat(plugA, has_one_pending_message());
		Assert.assertThat(plugA, has_pending_result("A"));
		
		plugA.play(plugB);
		
		Assert.assertEquals("A", pingResult.get());
	}

	@Test(timeout=TEST_TIMEOUT)
	public void verify_exception_passing() throws IOException, InterruptedException {
		NullPointerException e1 = new NullPointerException("HA-HA");
		Ping serverA = new FaultyServer(e1);
		Ping proxy = exportAtoB(serverA);
		
		plugA.autosend = plugB;
		plugB.autosend = plugA;
		
		Future<String> call = asyncSyncPing(proxy);
		
		try {
			call.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals(e1.toString(), e.getCause().toString());
		}
	}

	@Test(timeout=TEST_TIMEOUT)
	public void verify_exception_passing_async() throws IOException, InterruptedException, SecurityException, NoSuchMethodException {
		NullPointerException e1 = new NullPointerException("HA-HA");
		Ping serverA = new FaultyServer(e1);
		Ping proxy = exportAtoB(serverA);
		
		plugA.autosend = plugB;
		plugB.autosend = plugA;
		
		Future<String> call = asyncAsyncPing(proxy);
		
		try {
			call.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals(e1.toString(), e.getCause().toString());
		}
	}
	
	@Test(timeout=TEST_TIMEOUT)
	public void verify_outgoing_marshaling_failure_handling() throws IOException, InterruptedException, ExecutionException {
		PingServer serverA = new PingServer("XX");
		final Ping proxy = exportAtoB(serverA);

		Future<String> pingResult = asyncSyncPing(proxy);

		plugB.waitForMessage();
		
		Assert.assertThat(plugB, has_one_pending_message());
		Assert.assertThat(plugB, has_pending_call(Ping.class, "ping"));
		
		TestIOException e1 = new TestIOException("Test failure");
		plugB.failMessage(0,e1);

		// case 1, dynamic proxy returns actual exception 
		try {
			pingResult.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals(e1.toString(), e.getCause().toString());
		}

		pingResult = asyncSyncPing(proxy);

		plugB.waitForMessage();
		
		Assert.assertThat(plugB, has_one_pending_message());
		Assert.assertThat(plugB, has_pending_call(Ping.class, "ping"));
		
		IOException e2 = new IOException("Test failure");
		plugB.failMessage(0,e2);

		// case 2, dynamic proxy returns undeclared throwable 
		try {
			pingResult.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals(e2.toString(), ((UndeclaredThrowableException)e.getCause()).getUndeclaredThrowable().toString());
		}
		
		// make sure channel is functional
		verify_simple_call();		
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_incoming_marshaling_failure_handling() throws IOException, InterruptedException, ExecutionException {
		PingServer serverA = new PingServer("XX");
		final Ping proxy = exportAtoB(serverA);

		Future<String> pingResult = asyncSyncPing(proxy);

		plugB.waitForMessage();
		
		Assert.assertThat(plugB, has_one_pending_message());
		Assert.assertThat(plugB, has_pending_call(Ping.class, "ping"));

		plugB.play(plugA);

		plugA.waitForMessage();
		Assert.assertThat(plugA, has_one_pending_message());
		Assert.assertThat(plugA, has_pending_result("XX"));
		
		IOException e1 = new IOException("Test failure");
		plugA.failMessage(0, e1);
		plugA.waitForMessage();
		
		plugA.play(plugB);

		try {
			pingResult.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals("java.io.IOException: Failed to marshal call results: " + e1.toString(), ((UndeclaredThrowableException)e.getCause()).getUndeclaredThrowable().toString());
		}
		
		// make sure channel is functional
		verify_simple_call();		
	}	

	@Test(timeout=TEST_TIMEOUT)
	public void verify_task_abort_on_channel_destruction_1() throws IOException, InterruptedException, ExecutionException {
		PingServer serverA = new PingServer("XX");
		final Ping proxy = exportAtoB(serverA);

		// destroy before call is made
		sideB.destroy();

		Future<String> pingResult = asyncSyncPing(proxy);

		try {
			pingResult.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals("java.rmi.RemoteException: Channel [side-B] has been closed", ((UndeclaredThrowableException)e.getCause()).getUndeclaredThrowable().toString());
		}
	}	
	
	@Test(timeout=TEST_TIMEOUT)
	public void verify_task_abort_on_channel_destruction_2() throws IOException, InterruptedException, ExecutionException {
		PingServer serverA = new PingServer("XX");
		final Ping proxy = exportAtoB(serverA);

		Future<String> pingResult = asyncSyncPing(proxy);

		plugB.waitForMessage();
		
		Assert.assertThat(plugB, has_one_pending_message());
		Assert.assertThat(plugB, has_pending_call(Ping.class, "ping"));

		// destroy while call is pending
		sideB.destroy();
		
		try {
			pingResult.get();
			Assert.assertFalse("Exception expected", true);
		}
		catch(ExecutionException e) {
			Assert.assertEquals("java.rmi.RemoteException: Channel [side-B] has been closed", ((UndeclaredThrowableException)e.getCause()).getUndeclaredThrowable().toString());
		}
	}	
	
	private Future<String> asyncSyncPing(final Ping proxy) {
		Future<String> pingResult = es.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return proxy.ping();
			}
		});
		return pingResult;
	}

	private Future<String> asyncAsyncPing(final Ping proxy) throws SecurityException, NoSuchMethodException {
		RmiInvocationHandler rt = RemoteStub.getRmiChannel(proxy);
		Future<String> result = rt.invokeRemotely(proxy, Ping.class.getMethod("ping"));
		return result;
	}

	private Ping exportAtoB(Ping serverA) throws IOException {
		sideA.exportObject(Ping.class, serverA);
		Object rmiRef = sideA.writeReplace(serverA);
		final Ping proxy = (Ping) sideB.readResolve(rmiRef);
		return proxy;
	}

	private Matcher<PlugMock> has_one_pending_message() {		
		Matcher<PlugMock> qm = new BaseMatcher<PlugMock>() {

			@Override
			public boolean matches(Object pm) {
				return ((PlugMock)pm).queue.size() == 1;
			}

			@Override
			public void describeTo(Description desc) {
				desc.appendText("Queue has one pending call");				
			}			
		};
		return qm;
	}

	private Matcher<PlugMock> has_pending_call(final Class<Ping> type, final String method) {
		Matcher<PlugMock> qm = new BaseMatcher<PlugMock>() {

			@Override
			public boolean matches(Object pm) {
				for(Envelop e: ((PlugMock)pm).queue) {
					if (e.message instanceof RemoteCall) {
						RemoteCall rc = (RemoteCall) e.message;
						if (!rc.method.getClassName().equals(type.getName())) {
							continue;
						}
						if (!rc.method.getMethodName().equals(method)) {
							continue;
						}
						return true;
					}
				}
				return false;
			}

			@Override
			public void describeTo(Description desc) {
				desc.appendText("Queue has pending call ");
				desc.appendValue(type.getSimpleName() + "." + method);				
			}			
		};
		return qm;
	}

	private Matcher<PlugMock> has_pending_result(final Object result) {
		Matcher<PlugMock> qm = new BaseMatcher<PlugMock>() {
			
			@Override
			public boolean matches(Object pm) {
				for(Envelop e: ((PlugMock)pm).queue) {
					if (e.message instanceof RemoteReturn) {
						RemoteReturn rc = (RemoteReturn) e.message;
						if (rc.throwing) {
							continue;
						}
						if (result != null && rc.ret == null) {
							continue;
						}
						if (!result.equals(rc.ret)) {
							continue;
						}
						return true;
					}
				}
				return false;
			}
			
			@Override
			public void describeTo(Description desc) {
				desc.appendText("Queue has pending result ");
				desc.appendValue(result);				
			}			
		};
		return qm;
	}

	private interface Ping {
		public String ping() throws TestIOException;
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
	
	private SimpleClassProvider getClassProvider() {
		return new SimpleClassProvider(this.getClass().getClassLoader());
	}
	
	
	@SuppressWarnings("unused")
	private static class PlugMock implements DuplexObjectPipe {

		String name;
		List<Envelop> queue = new ArrayList<Envelop>();
		ObjectRecoder recoder;
		
		boolean closed;

		DuplexObjectPipe.ObjectReceiver receiver;
		PlugMock autosend;
		
		public PlugMock(String name) {
			this.name = name;
		}
		
		@Override
		public void bind(ObjectReceiver receiver) {
			this.receiver = receiver;
		}

		@Override
		public void close() {
			closed = true;
		}

		public void waitForMessage() throws InterruptedException {
			while(queue.isEmpty()) {
				Thread.sleep(100);
			}			
		}

		@Override
		public FutureEx<Void> sendObject(Object message) {
			FutureBox<Void> fb = new FutureBox<Void>();
			Envelop env = new Envelop(message, fb);
			queue.add(env);
			if (autosend != null) {
				play(autosend);
			}
			return fb;			
		}

		public void failMessage(int i, Exception e) {
			Envelop env = queue.remove(i);
			env.sendGuard.setError(e);
		}
		
		public void play(PlugMock sink) {
			List<Envelop> buf = new ArrayList<Envelop>(queue);
			queue.clear();
			for(Envelop e: buf) {
				try {
					e.sendGuard.setData(null);
					Object e2 = recoder.recodeAB(e.message);
					sink.receiver.objectReceived(e2);
				}
				catch(IllegalStateException ee) {
					// ignore
				}
			}
		}
		
		public String toString() {
			return "Plug[" + name + "]: " + queue;
		}
	}
	
	private static class Envelop {
		
		Object message;
		Box<Void> sendGuard;
		
		public Envelop(Object message, Box<Void> sendGuard) {
			this.message = message;
			this.sendGuard = sendGuard;
		}

		@Override
		public String toString() {
			return String.valueOf(message);
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
