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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.SocketStream;
import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.LogStream;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.gridkit.zerormi.zlog.ZLogger;

/**
 * This is an agent class initiating socket connection to RMI hub.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemotingEndPoint implements Runnable, RmiGateway.StreamErrorHandler {

	public static final String HEARTBEAT_PERIOD = "org.gridkit.telecontrol.slave.heart-beat-period";
	public static final String HEARTBEAT_TIMEOUT = "org.gridkit.telecontrol.slave.heart-beat-timeout";

//	private static final Logger LOGGER = LoggerFactory.getLogger(RemotingEndPoint.class);
	
	private static ZLogger LROOT = ZLogFactory.getDefaultRootLogger().getLogger("RemotingEndPoint");
	private static LogStream LTRACE = LROOT.get("", LogLevel.TRACE);
	private static LogStream LVERBOSE = LROOT.get("", LogLevel.VERBOSE);
	private static LogStream LINFO = LROOT.get("", LogLevel.INFO);
	private static LogStream LWARN = LROOT.get("", LogLevel.WARN);
	private static LogStream LERROR = LROOT.get("", LogLevel.CRITICAL);
	private static LogStream LFATAL = LROOT.get("", LogLevel.FATAL);
	
	
	private String uid;
	
	private RmiGateway gateway;
	
	private long pingInterval = Long.valueOf(System.getProperty(HEARTBEAT_PERIOD, "1000"));
	private long heartBeatTimeout = Long.valueOf(System.getProperty(HEARTBEAT_TIMEOUT, "60000"));
	private Object pingSingnal = new Object();

	private long lastHeartBeat = System.nanoTime();
	
	private DuplexStreamConnector connector;
	
	public RemotingEndPoint(String uid, SocketAddress addr) {
		this.uid = uid;
		this.connector = new ConnectSocketConnector(addr);
		this.gateway = new RmiGateway("master");
		this.gateway.setStreamErrorHandler(this);		
	}

	public RemotingEndPoint(String uid, DuplexStreamConnector connector) {
		this.uid = uid;
		this.connector = connector;
		this.gateway = new RmiGateway("master");
		this.gateway.setStreamErrorHandler(this);
	}
	
	public void enableHeartbeatDeatchWatch() {
		if (heartBeatTimeout != Integer.MAX_VALUE) {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						while(true) {
							Thread.currentThread().setName("HeatbeatDeathWatch-" + SimpleDateFormat.getDateTimeInstance().format(new Date()));
							long stale = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastHeartBeat);					
							if (stale > heartBeatTimeout) {
								System.err.println("Terminating process due to heartbeat timeout");
								System.err.flush();
								Runtime.getRuntime().halt(0);
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// ignore;
							}
						}
					}
					catch(Throwable e) {
						System.err.println("Unexpected exception in death watch thread " + e.toString());
						System.err.flush();
						Runtime.getRuntime().halt(0);
					}
				}
			};
			t.setDaemon(true);
			t.setName("HeartbeatDeathWatch");
			t.start();
		}
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
				
					LINFO.log("Connecting to master socket " + connector);
					final DuplexStream ss;
					
					try {
						ss = connector.connect();
					} catch (IOException e) {
						LFATAL.log("Connection has failed " + connector, e);
						return;
					}
					
					byte[] magic = uid.getBytes();
					ss.getOutput().write(magic);
					ss.getOutput().flush();

					LVERBOSE.log("Master socket connected");
					
					gateway.connect(ss);
					LVERBOSE.log("Gateway connected");
				}
				
				synchronized(pingSingnal) {
					pingSingnal.wait(pingInterval);
				}
				
				LTRACE.log("Ping");
				try {
					Future<?> f = gateway.getRemoteExecutorService().submit(new Ping());
					while(true) {
						try {
							f.get(5, TimeUnit.SECONDS);
							break;
						}
						catch(TimeoutException e) {
							long stale = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastHeartBeat);					
							if (stale > heartBeatTimeout) {
								System.err.println("Terminating process due to heartbeat timeout");
								System.err.flush();
								break;
							}
							
						}
					}
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
					LWARN.log("Ping failed: " + e.getCause().toString());
				}
			} catch (Exception e) {
				LERROR.log("Communication error %s", e);
			}
		}
		LINFO.log("Slave is disconting");
	}

	@Override
	public void streamError(DuplexStream socket, Object stream, Exception error) {
		LWARN.log("Slave read error: " + error.toString());
		synchronized(pingSingnal) {
			pingSingnal.notifyAll();
		}
		try {
			// TODO WTF?
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			LERROR.log("Stream error " + socket, e);
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
			LERROR.log("Stream error " + socket, e);
		}		
	}
	
	private static class ConnectSocketConnector implements DuplexStreamConnector {

		private final SocketAddress address;
		
		public ConnectSocketConnector(SocketAddress address) {
			this.address = address;
		}

		@Override
		public DuplexStream connect() throws IOException {
			Socket socket = new Socket();
			socket.connect(address);

			return new SocketStream(socket);
		}
		
		@Override
		public String toString() {
			return String.valueOf(address);
		}
	}	
}
