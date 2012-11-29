/**
 * Copyright 2012 Alexey Ragozin
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
package org.gridkit.vicluster.isolate.rmi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.gridkit.vicluster.ViGroup;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.junit.Test;

public class RmiIsolationTest {

	ViGroup hosts = new ViGroup();
	
	private IsolateViNode createIsolateViHost(String name) {
		IsolateViNode viHost = new IsolateViNode(name);
		hosts.addNode(viHost);
		return viHost;
	}

	/**
	 * Verifies that RMI will use correct context class loader for deserializing requests.
	 */
	@Test
	public void verify_isolate_connectivity() {
		
		ViNode node1 = createIsolateViHost("node1");
		ViNode node2 = createIsolateViHost("node2");
		
		ViGroup nodes = ViGroup.group(node1, node2);
		IsolateViNode.includePackage(nodes, "org.gridkit");
		
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
