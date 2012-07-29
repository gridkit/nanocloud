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
import java.net.Socket;
import java.net.SocketAddress;

import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.SocketStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an agent class initiating socket connection to RMI hub.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemotingEndPoint implements Runnable, RmiGateway.StreamErrorHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemotingEndPoint.class);
	
	private String uid;
	private SocketAddress addr;
	
	private RmiGateway gateway;
	
	private long pingInterval;
	private Object pingSingnal = new Object();
	
	public RemotingEndPoint(String uid, SocketAddress addr) {
		this.uid = uid;
		this.addr = addr;
		this.gateway = new RmiGateway();
		this.gateway.setStreamErrorHandler(this);
	}
	
	public void run() {
		while(true) {
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				// ignore
			}
			
			try {
				if (!gateway.isConnected()) {
				
					final Socket sock = new Socket();
					
					try {
						sock.connect(addr);
					} catch (IOException e) {
						LOGGER.error("Connection has failed", addr);
						return;
					}
					
					byte[] magic = uid.getBytes();
					sock.getOutputStream().write(magic);

					DuplexStream ss = new SocketStream(sock);
					
					gateway.connect(ss);
				}
				
				synchronized(pingSingnal) {
					pingSingnal.wait(pingInterval);
				}
				
				LOGGER.debug("Ping");
				gateway.getRemoteExecutorService().submit(new Ping()).get();
			} catch (Exception e) {
				LOGGER.error("Communication error", e);
			}
		}		
	}

	@Override
	public void streamError(DuplexStream socket, Object stream, Exception error) {
		synchronized(pingSingnal) {
			pingSingnal.notifyAll();
		}
		try {
			socket.close();
		} catch (IOException e) {
			LOGGER.error("Stream error " + socket, e);
		}
	}
}
