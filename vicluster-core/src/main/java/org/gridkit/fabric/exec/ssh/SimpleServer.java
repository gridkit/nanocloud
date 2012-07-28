/**
 * Copyright 2008-2009 Grid Dynamics Consulting Services, Inc.
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
package org.gridkit.fabric.exec.ssh;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SimpleServer implements Runnable {
	
	private static Logger logger = Logger.getLogger(SimpleServer.class.getName());
	
	private ServerSocket socket;
	
	private Map<String, Socket> connectionMap = new HashMap<String, Socket>();
	
	public SimpleServer() throws IOException {
		this(0);
	}

	public SimpleServer(int port) throws IOException {
		socket = new ServerSocket();
		SocketAddress addr = new InetSocketAddress(port);
		socket.bind(addr);
	}
	
	public InetSocketAddress getServerAddress() {
		return (InetSocketAddress) socket.getLocalSocketAddress();
	}
	
	public void start() {
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}
	
	public Socket getConnection(String id, long timeout) throws InterruptedException {
		synchronized(connectionMap) {
			long deadline = System.nanoTime() + timeout;
			while(true) {
				Socket socket = connectionMap.get(id);
				if (socket != null) {
					return socket;
				}
				long waitTime = deadline - System.nanoTime();
				if (timeout == 0 || waitTime > 0) {
					connectionMap.wait(TimeUnit.NANOSECONDS.toMillis(waitTime));
					continue;
				}
				return null;
			}
		}
	}

	public void closeConnection(String id) {
		synchronized(connectionMap) {
			Socket socket = connectionMap.get(id);
			if (socket != null) {
				connectionMap.remove(id);
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	public void run() {
		
		String name = Thread.currentThread().getName();
		Thread.currentThread().setName("ConnectionServer");
		try {
			
			while(true) {
				Socket connection = socket.accept();
				try {
					InputStream is = connection.getInputStream();
					String connectionId = new DataInputStream(is).readUTF();
					logger.info("Client connected " + connection.getRemoteSocketAddress().toString() + " ID:" + connectionId);
					synchronized(connectionMap) {
						if (connectionMap.containsKey(connectionId)) {
							logger.info("Duplicated connection ID, terminating " + connection.getRemoteSocketAddress().toString() + " ID:" + connectionId);
							connection.close();
						}
						connectionMap.put(connectionId, connection);
						connectionMap.notifyAll();
					}
				}
				catch(IOException e) {
					logger.info("Connection failed " + connection.getRemoteSocketAddress().toString() + " " + e.toString());
				}
			}
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "", e);
			System.exit(1);
		}
		finally {
			Thread.currentThread().setName(name);
		}
	}
}
