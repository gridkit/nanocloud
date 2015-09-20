/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.zerormi;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.FutureEx;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("serial")
public class RmiChannelPipeTest {

	DuplexStream leftSock;
	DuplexStream rightSock;

	{
		Object[] pair1 = createSocketPair();
		InputStream leftIn = (InputStream) pair1[0];
		OutputStream rightOut = (OutputStream) pair1[1];
		Object[] pair2 = createSocketPair();
		InputStream rightIn = (InputStream) pair2[0];
		OutputStream leftOut = (OutputStream) pair2[1];
		leftSock = new NamedStreamPair("LEFT", leftIn, leftOut);
		rightSock = new NamedStreamPair("RIGHT", rightIn, rightOut);
	}

	RmiGateway left;
	RmiGateway right;	
	
	{
		Thread leftStarter = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					RmiGateway l = new RmiGateway("l");
					l.connect(leftSock);
					left = l;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		Thread rightStarter = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					RmiGateway r = new RmiGateway("r");
					r.connect(rightSock);
					right = r;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		leftStarter.start();
		rightStarter.start();
		
		try {
			leftStarter.join();
			rightStarter.join();
		} catch (InterruptedException e) {
		}		
	}
	
	@After
	public void stopRmi() {
		left.shutdown();
		right.shutdown();
	}
	
	private Object[] createSocketPair() {
		while(true) {
			try {
				int port = 10000 + new Random().nextInt(10000);
				SocketAddress saddr = new InetSocketAddress("127.0.0.1", port); 
				ServerSocket ss = new ServerSocket();
				ss.bind(saddr);
				Socket sock = new Socket();
				sock.connect(saddr);
				Socket rsock = ss.accept();
				
				return new Object[]{sock.getInputStream(), rsock.getOutputStream()};
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	public void ping() throws InterruptedException, ExecutionException {
	    Future<String> task = left.getRemoteExecutorService().submit(new Echo<String>("ping"));
	    Assert.assertEquals("ping", task.get());	    
	}
	
	@Test
	public void simple_test() throws InterruptedException, IOException, ExecutionException {
		Future<String> task = left.getRemoteExecutorService().submit(new Echo<String>("abc"));
		Assert.assertEquals("abc", task.get());
	}
	
	@Test
	public void verify_serialization() throws InterruptedException, IOException, ExecutionException {
		SelfIdentity sid = new SelfIdentity();
		Future<SelfIdentity> task = left.getRemoteExecutorService().submit(sid);
		Assert.assertNotSame(sid, task.get());
	}

	@Test
	public void fail_on_non_serializable() throws InterruptedException, IOException, ExecutionException {
        try {
            Future<String> task = left.getRemoteExecutorService().submit(new NotSerializable());
            task.get();
            Assert.fail("Exception expected");
        }
        catch(ExecutionException e) {
            Assert.assertTrue(e.getCause() instanceof RemoteException);
            Assert.assertTrue(e.getCause().getCause() instanceof NotSerializableException);
        }
        ping();
	}

	@Test
	public void fail_on_non_serializable2() throws InterruptedException, IOException, ExecutionException {
	    try {
    		Future<String> task = left.getRemoteExecutorService().submit(new SerializableAdapter<String>(new NotSerializable()));
    		task.get();
    		Assert.fail("Exception expected");
	    }
	    catch(ExecutionException e) {
	        Assert.assertTrue(e.getCause() instanceof RemoteException);
	        Assert.assertTrue(e.getCause().getCause() instanceof NotSerializableException);
	    }
	    ping();
	}

	@Test
	public void fail_on_non_serializable_return_value() throws InterruptedException, IOException, ExecutionException {
	    try {
	        Future<NotSerializable> task = left.getRemoteExecutorService().submit(new Callable<NotSerializable>() {
                @Override
                public NotSerializable call() throws Exception {
                    return new NotSerializable();
                }
            });
	        task.get();
	        Assert.fail("Exception expected");
	    }
	    catch(ExecutionException e) {
	        e.printStackTrace();
	        Assert.assertTrue(e.getCause() instanceof RemoteException);
	        Assert.assertTrue(e.getCause().getCause() instanceof NotSerializableException);
	    }
	    ping();
	}
	    
	
	@Test
	public void verify_auto_export() throws InterruptedException, IOException, ExecutionException {
		Future<String> task = left.getRemoteExecutorService().submit(new ProxyAdapter<String>(new NotSerializable()));
		Assert.assertEquals("NotSerializable", task.get());
	}

	@Test
	public void verify_async_proxy_call() throws InterruptedException, IOException, ExecutionException, SecurityException, NoSuchMethodException {
	    ProxyCallable<String> proxy = left.getRemoteExecutorService().submit(new Callable<ProxyCallable<String>>() {

            @Override
            public ProxyCallable<String> call() throws Exception {
                return new ProxyAdapter<String>(new NotSerializable());
            }
	        
	    }).get();
	    
	    FutureEx<String> result = RemoteStub.remoteSubmit(proxy, Callable.class.getMethod("call"));
	    Assert.assertEquals("NotSerializable", result.get());
	}
	
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
}
