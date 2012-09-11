package org.gridkit.zerormi;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.DuplexObjectPipe.ObjectReceiver;
import org.junit.Before;
import org.junit.Test;

public class SmartObjectSerializerTest {

	private PipeMock pipe;
	private SmartObjectSerializer serializer;
	private List<Object> output = new ArrayList<Object>();
	
	@Before
	public void init() {
		pipe = new PipeMock();
		RmiMarshaler marshaler = new SmartRmiMarshaler();
		serializer = new SmartObjectSerializer(pipe, marshaler, new SimpleClassProvider(this.getClass().getClassLoader()));
		serializer.bind(new ObjectReceiver() {
			
			@Override
			public FutureEx<Void> objectReceived(Object object) {
				output.add(object);
				FutureBox<Void> ack = new FutureBox<Void>();
				ack.setData(null);
				return ack;
			}
			
			@Override
			public void closed() {
				// do nothing				
			}
		});
	}
	
	@Test
	public void verify_string_round_trip() throws InterruptedException, ExecutionException {
		Future<Void> ack = serializer.sendObject("Text");
		//serializer.process(10, TimeUnit.MILLISECONDS);
		Assert.assertEquals(1, pipe.buffer.size());
		reflect();
		Assert.assertEquals("Text", output.get(0));
		ack.get();
	}
	
	@Test
	public void verify_string_array_round_trip() throws InterruptedException, ExecutionException {
		String[] array = {"ABC", "123"};
		Future<Void> ack = serializer.sendObject(array);
		//serializer.process(10, TimeUnit.MILLISECONDS);
		Assert.assertEquals(1, pipe.buffer.size());
		reflect();
		String[] result = (String[]) output.get(0);
		Assert.assertEquals("[ABC, 123]", Arrays.toString(result));
		ack.get();
	}
	
	@Test
	public void verify_anon_class_passing() throws InterruptedException, ExecutionException {
		final String text = "Text";
		final String[] textArray = {"1", "2"};
		final int intVal = 10;
		final int[] intArray = {1, 2};
		Object annon = new Object() {
			@Override
			public String toString() {
				String x = text + textArray + intVal + intArray;
				return text + (x.length() / Integer.MAX_VALUE);
			}
		};

		Future<Void> ack = serializer.sendObject(annon);
		//serializer.process(10, TimeUnit.MILLISECONDS);
		Assert.assertEquals(1, pipe.buffer.size());
		reflect();
		Assert.assertEquals("Text0", output.get(0).toString());
		ack.get();
	}

	@Test
	public void verify_dynamic_proxy_passing() throws InterruptedException, ExecutionException {
		
		Ping proxy = (Ping) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Ping.class}, new PingHandler());
		
		Future<Void> ack = serializer.sendObject(proxy);
		//serializer.process(10, TimeUnit.MILLISECONDS);
		Assert.assertEquals(1, pipe.buffer.size());
		reflect();
		Assert.assertEquals("pong", ((Ping)output.get(0)).ping());
		ack.get();
	}
	
	private void reflect() throws InterruptedException, ExecutionException {
		pipe.reflect();		
	}

	public static interface Ping {
		
		public String ping();
		
	}
		
	@SuppressWarnings("serial")
	private static class PingHandler implements InvocationHandler, Serializable {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("ping")) {
				return "pong";
			}
			else {
				return method.invoke(proxy, args);
			}
		}
	}
	
	private static class PipeMock implements DuplexBlobPipe {

		List<Envelop> buffer = new ArrayList<Envelop>();
		BlobReceiver receiver;
		
		@Override
		public void bind(BlobReceiver receiver) {
			this.receiver = receiver;
		}

		public void reflect() throws InterruptedException, ExecutionException {
			List<Envelop> list = new ArrayList<Envelop>(buffer);
			buffer.clear();	
			for(Envelop e: list) {
				e.ack.setData(null);
				receiver.blobReceived(e.buf).get();
			}
		}
		
		@Override
		public FutureEx<Void> sendBinary(byte[] bytes) {
			Envelop e = new Envelop();
			e.buf = bytes;
			e.ack = new FutureBox<Void>();
			buffer.add(e);
			return e.ack;
		}

		@Override
		public void close() {
		}
	}
	
	private static class Envelop {
		
		byte[] buf;
		FutureBox<Void> ack;
		
	}
}
