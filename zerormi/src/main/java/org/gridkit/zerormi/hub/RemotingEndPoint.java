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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.util.concurrent.TaskService.Task;
import org.gridkit.zerormi.AbstractSuperviser;
import org.gridkit.zerormi.ByteStream;
import org.gridkit.zerormi.ByteStream.Duplex;
import org.gridkit.zerormi.ReliableBlobPipe;
import org.gridkit.zerormi.ReliableBlobPipe.PipeSuperviser;
import org.gridkit.zerormi.RmiFactory;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an agent class initiating socket connection to RMI hub.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemotingEndPoint extends AbstractSuperviser implements Runnable, PipeSuperviser, Executor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemotingEndPoint.class);
	
	private String uid;
	private SocketFactory target;
	
	private RmiGateway gateway;
	private ReliableBlobPipe pipe;
	private TaskService.Component taskService;
	private Socket socket;
	
	private long pingInterval = Long.valueOf(System.getProperty("org.gridkit.telecontrol.slave.heart-beat-period", "1000"));
	private long heartBeatTimeout = Long.valueOf(System.getProperty("org.gridkit.telecontrol.slave.heart-beat-timeout", "60000"));
	private Object pingSingnal = new Object();

	private long lastHeartBeat = System.nanoTime(); 
	
	public RemotingEndPoint(String name, String uid, SocketFactory target) {
		super(name);
		this.uid = uid;
		this.target = target;
				
		this.taskService = new SensibleTaskService(name);
		this.pipe = new ReliableBlobPipe("pipe:" + name, this);
		
		this.gateway = RmiFactory.createEndPoint(name, pipe, this);
		
		addComponent(pipe);
		addComponent(taskService);
	}
	
	public void enableHeartbeatDeatchWatch() {
		heartBeatTimeout = System.nanoTime();
		Thread t = new Thread() {
			@Override
			public void run() {
				while(true) {
					Thread.currentThread().setName("HeartbeatDeathWatch-" + SimpleDateFormat.getDateTimeInstance().format(new Date()));
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
		t.setName("HeartbeatDeathWatch");
		t.start();
	}
	
	public void run() {
		try {
			while(true) {
				
				if (terminated) {
					return;
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// ignore
				}
				
				try {
					
					reconnect();
					
					synchronized(pingSingnal) {
						pingSingnal.wait(pingInterval);
					}
					
					LOGGER.debug("Pinging ...");
					try {
						gateway.asExecutor().submit(new Ping()).get();
						lastHeartBeat = System.nanoTime();
					}
					catch(ExecutionException e) {
						if (!isConnected()) {
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
		finally {
			terminate();
		}
	}

	public void shutdown() {
		terminate();
	}
	
	private void reconnect() {
		try {
			synchronized(this) {
				if (!isConnected()) {
				
					if (socket != null) {
						components.remove(socket);
					}
					
					LOGGER.info("Connecting to master socket");
					try {
						socket = target.connect();
					} catch (IOException e) {
						LOGGER.error("Cannot establish connection [" + target + "]", e);
						return;
					}
					
					try {
						addComponent(socket);
					}
					catch(IllegalStateException e) {
						// terminated
						try {
							socket.close();
						}
						catch(IOException ee) {
							// ignore;
						}
						return;
					}
					
					byte[] magic = uid.getBytes();
					socket.getOutputStream().write(magic);
					socket.getOutputStream().flush();
	
					LOGGER.info("Master socket connected");
					ByteStream.Duplex ss = Streams.toDuplex(socket, taskService);
					
					try {
						pipe.setStream(ss);
					}
					catch(IllegalStateException e) {
						// this means pipe is terminated
						// just return and let it die
						return;
					}
					LOGGER.info("Gateway connected");
				}
			}
		}
		catch(IOException e) {
			LOGGER.error("Communication error", e);
			stopAll();
		}
	}

	private synchronized boolean isConnected() {
		return socket != null && socket.isConnected() && !socket.isClosed();
	}

	@Override
	public void execute(final Runnable command) {
		taskService.schedule(new Task(){

			@Override
			public void run() {
				command.run();				
			}

			@Override
			public void interrupt(Thread taskThread) {
				taskThread.interrupt();				
			}

			@Override
			public void cancled() {
				// do nothing				
			}			
		});		
	}

	@Override
	public void onStreamRejected(ReliableBlobPipe pipe, Duplex stream,	Exception e) {
		LOGGER.info("[" + name + "] Stream rejected, will reconnect. ", e);
		disposeSocket();
		reconnect();		
	}

	private synchronized void disposeSocket() {
		if (socket != null) {
			try {
				if (!socket.isInputShutdown()) {
					socket.shutdownInput();
				}
				else if (!socket.isOutputShutdown()) {
					socket.shutdownOutput();
				}
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException e1) {
					//ignore
				}
			}
			socket = null;
			components.remove(socket);
		}
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected Logger getLogger(Object component) {
		String name = getClass().getName() + component.getClass().getSimpleName();
		return LoggerFactory.getLogger(name);
	}

	@Override
	protected Object safeStatus(Object obj) {
		if (obj instanceof Socket) {
			Socket sock = (Socket)obj;
			return "connected=" + sock.isConnected() +" ,closed=" + sock.isClosed();
		}
		return super.safeStatus(obj);
	}
	
	@Override
	protected void stop(Object obj) {
		if (obj instanceof RmiGateway) {
			((RmiGateway)obj).shutdown();
		}
		else if (obj instanceof TaskService.Component) {
			((TaskService.Component)obj).shutdown();
		}
		else if (obj instanceof Socket) {
			try {
				((Socket)obj).close();
			} catch (IOException e) {
				// ignore
			}
		}
		else {
			super.stop(obj);
		}
	}
	
	
}
