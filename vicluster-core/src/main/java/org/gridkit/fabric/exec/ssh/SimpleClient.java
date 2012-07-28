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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


class SimpleClient implements Runnable {
	
	private static Logger logger = Logger.getLogger(SimpleClient.class.getName());
	
	private String host;
	private int port;
	private OutputStream outstream;
	
	public SimpleClient(String host, int port) {
		this.port = port;
		this.host = host;
	}
	
	public void start() {
		Thread thread = new Thread(this);
		thread.setName("OffspringPinger");
		thread.start();
	}
	
	public synchronized void send(String message) throws IOException {
		while(outstream == null) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		outstream.write((message + "\n").getBytes()); 
	}
	
	
	public void run() {
				
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(host, port));
			
			synchronized(this) {
				outstream = socket.getOutputStream();
				notifyAll();
				
				while(true) {
					try {
						wait(200);
					} catch (InterruptedException e) {
						// ignore
					}
					send(""); // send ping
				}
			}
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
		finally {
			logger.severe("Detached from master, kill myself");
			System.exit(1);
		}
	}
}
