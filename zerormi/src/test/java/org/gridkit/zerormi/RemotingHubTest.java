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
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.zerormi.hub.LegacySpore;
import org.gridkit.zerormi.hub.RemotingEndPoint;
import org.gridkit.zerormi.hub.RemotingHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.gridkit.zerormi.hub.SimpleSocketAcceptor;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RemotingHubTest {

	int hubPort;
	
	private RemotingHub hub;
	private RemotingEndPoint endPoint1;
	private RemotingEndPoint endPoint2;
	private SimpleSocketAcceptor acceptor;
	private AdvancedExecutor remoteExecutor1;
	private AdvancedExecutor remoteExecutor2;
	
	@Before
	public void initHub() throws InterruptedException, BrokenBarrierException, TimeoutException {
		hub = new RemotingHub(ZLogFactory.getDefaultRootLogger());
		
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
		
		String uid1 = LegacySpore.uidOf(hub.allocateSession("side1", sessionListener));
		String uid2 = LegacySpore.uidOf(hub.allocateSession("side2", sessionListener));
		
		
		acceptor = new SimpleSocketAcceptor();
		ServerSocket ssock = openServerSocket();
		
		acceptor.bind(ssock, hub);
		acceptor.start();
		
		endPoint1 = new RemotingEndPoint(uid1, new InetSocketAddress("localhost", hubPort));
		new Thread(endPoint1).start();
		
		remoteExecutor1 = hub.getExecutionService(uid1);

		endPoint2 = new RemotingEndPoint(uid2, new InetSocketAddress("localhost", hubPort));
		new Thread(endPoint2).start();
		
		remoteExecutor2 = hub.getExecutionService(uid2);
		
		latch.await(5000000, TimeUnit.MILLISECONDS);		
	}

	protected ServerSocket openServerSocket() {
		ServerSocket ssock;
		try {
			ssock = new ServerSocket(0);
			hubPort = ssock.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ssock;
	}
	
	@After
	public void shutdown() {
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
