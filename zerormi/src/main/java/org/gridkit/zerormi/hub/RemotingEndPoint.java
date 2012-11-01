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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

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

	public static final String HEARTBEAT_PERIOD = "org.gridkit.telecontrol.slave.heart-beat-period";
	public static final String HEARTBEAT_TIMEOUT = "org.gridkit.telecontrol.slave.heart-beat-timeout";

	private static final Logger LOGGER = LoggerFactory.getLogger(RemotingEndPoint.class);
	
	private String uid;
	private SocketAddress addr;
	
	private RmiGateway gateway;
	
	private long pingInterval = Long.valueOf(System.getProperty(HEARTBEAT_PERIOD, "1000"));
	private long heartBeatTimeout = Long.valueOf(System.getProperty(HEARTBEAT_TIMEOUT, "60000"));
	private Object pingSingnal = new Object();

	private long lastHeartBeat = System.nanoTime(); 
	
	public RemotingEndPoint(String uid, SocketAddress addr) {
		this.uid = uid;
		this.addr = addr;
		this.gateway = new RmiGateway("master");
		this.gateway.setStreamErrorHandler(this);
	}
	
	public void enableHeartbeatDeatchWatch() {
		Thread t = new Thread() {
			@Override
			public void run() {
				while(true) {
					Thread.currentThread().setName("HeatbeatDethWatch-" + SimpleDateFormat.getDateTimeInstance().format(new Date()));
					long stale = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastHeartBeat);					
					if (stale > heartBeatTimeout) {
						System.err.println("Terminating process due to heartbeat timeout");
						Runtime.getRuntime().halt(0);
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// ignore;
					}
				}
			}
		};
		t.setDaemon(true);
		t.setName("HeatbeatDethWatch");
		t.start();
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
				
					LOGGER.info("Connecting to master socket");
					final Socket sock = new Socket();
					
					try {
						sock.connect(addr);
					} catch (IOException e) {
						LOGGER.error("Connection has failed", addr);
						return;
					}
					
					byte[] magic = uid.getBytes();
					sock.getOutputStream().write(magic);
					sock.getOutputStream().flush();

					LOGGER.debug("Master socket connected");
					DuplexStream ss = new SocketStream(sock);
					
					gateway.connect(ss);
					LOGGER.debug("Gateway connected");
				}
				
				synchronized(pingSingnal) {
					pingSingnal.wait(pingInterval);
				}
				
				LOGGER.trace("Ping");
				try {
					gateway.getRemoteExecutorService().submit(new Ping()).get();
					lastHeartBeat = System.nanoTime();
				}
				catch(RejectedExecutionException e) {
					// shutting down
					break;
				}
				catch(ExecutionException e) {
					if (!gateway.isConnected()) {
						break;
					}
					LOGGER.warn("Ping failed: " + e.getCause().toString());
				}
			} catch (Exception e) {
				LOGGER.error("Communication error", e);
			}
		}
		LOGGER.info("Slave has been discontinued");
	}

	@Override
	public void streamError(DuplexStream socket, Object stream, Exception error) {
		LOGGER.warn("Slave read error: " + error.toString());
		synchronized(pingSingnal) {
			pingSingnal.notifyAll();
		}
		try {
			// TODO WTF?
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			LOGGER.error("Stream error " + socket, e);
		}
	}

	@Override
	public void streamClosed(DuplexStream socket, Object stream) {
		synchronized(pingSingnal) {
			pingSingnal.notifyAll();
		}
		try {
			// TODO WTF?
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			LOGGER.error("Stream error " + socket, e);
		}		
	}
}
