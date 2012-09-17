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

import java.io.Closeable;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.util.concurrent.SimpleTaskService;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.util.concurrent.TaskService.Task;
import org.gridkit.zerormi.AbstractSuperviser;
import org.gridkit.zerormi.ByteStream.Duplex;
import org.gridkit.zerormi.ClassProvider;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.ReliableBlobPipe;
import org.gridkit.zerormi.ReliableBlobPipe.PipeSuperviser;
import org.gridkit.zerormi.RmiFactory;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.SimpleClassProvider;
import org.gridkit.zerormi.SmartRmiMarshaler;
import org.gridkit.zerormi.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a hub managing multiple RMI channel connection.
 * It accepts duplex stream, verifies and matches ID token.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemotingHub {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(RemotingHub.class);

	private final static int UID_LENGTH = 32;
	
	private SecureRandom srnd ;
	private TaskService taskService;
	private ConcurrentMap<String, SessionContext> connections = new ConcurrentHashMap<String, SessionContext>();
	
	public RemotingHub() {
		this(new SensibleTaskService("remoting-hub"));
	}
	
	public RemotingHub(TaskService taskService) {
		this.taskService = taskService;
		try {
			srnd = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String newSession(String name, SessionEventListener listener) {
		while(true) {
			String uid = generateUID();
			SessionContext ctx = new SessionContext(name);
			ctx.listener = listener;			
			synchronized(ctx) {
				if (connections.putIfAbsent(uid, ctx) != null) {
					continue;
				}
				ctx.pipe = new ReliableBlobPipe("pipe:" + name, ctx);
				ClassProvider cp = new SimpleClassProvider(Thread.currentThread().getContextClassLoader());
				ctx.gateway = RmiFactory.createEndPoint(name, cp, ctx.pipe, ctx, new SmartRmiMarshaler());				
				ctx.addComponent(ctx.pipe);
				ctx.addComponent(ctx.gateway);
				
			}
			return uid;
		}		
	}
	
	public RmiGateway getGateway(String sessionId) {
		SessionContext ctx = connections.get(sessionId);
		if (ctx != null) {
			return ctx.gateway;
		}
		else {
			return null;
		}
	}
	
	private String generateUID() {
		byte[] magic = new byte[UID_LENGTH / 2];
		srnd.nextBytes(magic);
		StringBuilder sb = new StringBuilder();
		for(byte b:magic) {
			sb.append(Integer.toHexString(0xF & (b >> 4)));
			sb.append(Integer.toHexString(0xF & b));
		}
		return sb.toString();
	}

	public void closeAllConnections() {
		try {
			while(true) {
				String id = connections.keySet().iterator().next();
				closeConnection(id);
			}
		}
		catch(NoSuchElementException e) {
			// done
		}
	}
	
	public void closeConnection(String id) {
		SessionContext ctx = connections.get(id);
		if (ctx != null) {
			synchronized(ctx) {
				ctx = connections.get(id);
				if (ctx != null) {
					ctx.listener.closed();
					ctx.gateway.shutdown();
					ctx.shutdown();
					connections.remove(id);
					
					// done
					return;
				}
			}
		}
		throw new IllegalArgumentException("Connection not found " + id);
	}
	
	public void dispatch(DuplexStream stream) {
		String id = readId(stream);
		if (id != null) {
			SessionContext ctx = connections.get(id);
			if (ctx != null) {
				synchronized(ctx) {
					ctx = connections.get(id);
					if (ctx != null) {
						if (ctx.socket != null) {
							LOGGER.warn("[" + ctx.getName() + "] New stream for " + id + " " + stream);
							LOGGER.warn("[" + ctx.getName() + "] Old stream for " + id + " would be disposed " + ctx.socket);
							ctx.pipe.setStream(null);
							ctx.disposeSocket();
						}
						try {
							ctx.addComponent(stream);
							ctx.pipe.setStream(Streams.toDuplex(stream, taskService));
							ctx.socket = stream;
							ctx.listener.connected(stream);
						} catch (IOException e) {
							LOGGER.error("Stream connection failed " + stream);
						}
						LOGGER.info("Stream connected at end point " + id + " - " + stream);
						return;
					}
				}
			}
		}
		LOGGER.warn("Stream were not connected " + stream);
		silentClose(stream);
	}
	
	private String readId(DuplexStream stream) {
		try {
			byte[] magic = new byte[UID_LENGTH];
			for(int i = 0; i != magic.length; ++i) {
				magic[i] = (byte) stream.getInput().read();
			}
			return new String(magic);
		} catch (IOException e) {
			return null;
		}
	}

	public interface SessionEventListener {
		
		public void connected(DuplexStream stream);
		
		public void interrupted(DuplexStream stream);
		
		public void reconnected(DuplexStream stream);
		
		public void closed();		
	}
	
	private class SessionContext extends AbstractSuperviser implements PipeSuperviser, Executor {
		
		private SessionEventListener listener;
		private RmiGateway gateway;
		private ReliableBlobPipe pipe;
		private DuplexStream socket;
		
		public SessionContext(String name) {
			super(name);
		}
		
		public void shutdown() {
			terminate();
		}
		
		@Override
		protected Logger getLogger() {
			return LOGGER;
		}
		
		@Override
		protected Logger getLogger(Object component) {
			String name = RemotingHub.class.getName() + "." + component.getClass().getSimpleName();
			return LoggerFactory.getLogger(name);
		}

		@Override
		public void onStreamRejected(ReliableBlobPipe pipe, Duplex stream,	Exception e) {
			LOGGER.info("[" + name + "] Stream rejected, will reconnect. ", e);
			disposeSocket();
		}
		
		private synchronized void disposeSocket() {
			if (socket == null) {
				return;
			}
			else {
				try {
					socket.close();
				}
				catch(IOException e) {
					// ignore;
				}
			}
			components.remove(socket);
			listener.interrupted(socket);
			socket = null;
		}

		@Override
		protected void stop(Object obj) {
			if (obj instanceof DuplexStream) {
				try {
					((DuplexStream)obj).close();
				} catch (IOException e) {
					// ignore
				}
			}
			else if (obj instanceof RmiGateway) {
				((RmiGateway)obj).shutdown();
			}
			else if (obj instanceof SimpleTaskService) {
				((SimpleTaskService)obj).shutdown();
			}
			else {
				super.stop(obj);
			}
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
	}
	
	private static final void silentClose(Closeable ch) {
		try {
			ch.close();
		} catch (IOException e) {
			// ignore
		}
	}
}
