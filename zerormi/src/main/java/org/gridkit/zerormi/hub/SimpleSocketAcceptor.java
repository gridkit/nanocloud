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
package org.gridkit.zerormi.hub;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.SocketStream;

/**
 * This is a simple socket server passing incoming connections to {@link RemotingHub} 
 * which authenticates them and processing RMI handshake. 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SimpleSocketAcceptor implements Runnable {

	private RemotingHub hub;
	private ServerSocket socket;
	private Thread acceptor;
	
	public SimpleSocketAcceptor() {		
	}
	
	public void bind(ServerSocket socket, RemotingHub hub) {
		this.socket = socket;
		this.hub = hub;
	}
	
	public void start() {
		acceptor = new Thread(this);
		acceptor.start();
	}

	@Override
	public void run() {
		try {
			while(true) {
				Socket con =socket.accept();
				// TODO logging
				System.out.println("Connection accepted: " + con.getRemoteSocketAddress());
				DuplexStream ds = new SocketStream(con);
				hub.dispatch(ds);
			}
		} catch (IOException e) {
			if (!socket.isClosed()) {
				e.printStackTrace();
			}
		}
	}
	
	public void close() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
