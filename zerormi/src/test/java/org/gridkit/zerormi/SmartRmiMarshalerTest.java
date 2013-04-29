package org.gridkit.zerormi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class SmartRmiMarshalerTest {

	@Test
	public void test_serialization() throws IOException, ClassNotFoundException {				
		
		final Object taskId = "task";
		final int subId = 10;
		Runnable task = new Runnable() {
			int x = 100;
			
			@Override
			public void run() {
			}
			
			public String toString() {
				return taskId + "." + subId + "." + x;
			}
		};
		
		Object ms = SmartAnonMarshaler.marshal(task);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(ms);
		oos.close();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bis);
		Object md = ois.readObject();
		
		Assert.assertEquals("task.10.100", SmartAnonMarshaler.unmarshal(md).toString());		
	}		
	
	@Test
	public void check_PingServerA() throws IOException {
		assertRemotes(PingServerA.class, PingA.class);
	}

	@Test
	public void check_PingServerB() throws IOException {
		assertRemotes(PingServerB.class, PingA.class);
	}

	@Test
	public void check_PingServerC() throws IOException {
		assertRemotes(PingServerC.class, PingC.class, PingA.class);
	}
	
	@Test
	public void check_PingServerX() throws IOException {
		assertRemotes(PingServerX.class, PingX.class);
	}

	@Test
	public void check_PingServerXX() throws IOException {
		assertRemotes(PingServerXX.class, PingX.class, PingC.class);
	}
	
	void assertRemotes(Class<?> target, Class<?>... expected) throws IOException {
		SmartRmiMarshaler marshaler = new SmartRmiMarshaler();
		Assert.assertEquals(Arrays.toString(expected), Arrays.toString(marshaler.detectRemoteInterfaces(target)));
	}
	

	@Test(expected = IOException.class)
	public void check_PingUnserver() throws IOException {
		SmartRmiMarshaler marshaler = new SmartRmiMarshaler();
		
		marshaler.detectRemoteInterfaces(PingUnserver.class);
	}
	
	public interface PingA extends Remote {
		
		public void ping();
		
	}

	public interface PingC extends Remote {
		
		public void ping();
		
	}
	
	public interface PingX extends PingA {
		
	}
	
	public static class PingUnserver {
		
	}
	
	public static class PingServerA implements PingA {

		@Override
		public void ping() {
		}
	}

	public static class PingServerB extends PingServerA {
		
		@Override
		public void ping() {
		}
	}

	public static class PingServerC extends PingServerB implements PingC {
		
		@Override
		public void ping() {
		}
	}

	public static class PingServerX implements PingX {
		
		@Override
		public void ping() {
		}
	}

	public static class PingServerXX extends PingServerC implements PingX {
		
		@Override
		public void ping() {
		}
	}
}
