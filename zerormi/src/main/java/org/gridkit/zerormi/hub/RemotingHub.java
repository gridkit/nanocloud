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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.RmiGateway.StreamErrorHandler;
import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.LogStream;
import org.gridkit.zerormi.zlog.ZLogger;

/**
 * This is a hub managing multiple RMI channel connection.
 * It accepts duplex stream, verifies and matches ID token.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemotingHub implements MasterHub {
	
	private final static int UID_LENGTH = 32;
	
	private LogStream logInfo;
	private LogStream logWarn;
	private LogStream logError;
	private SecureRandom srnd ;	
	private ConcurrentMap<String, SessionContext> connections = new ConcurrentHashMap<String, SessionContext>();
	
	public RemotingHub(ZLogger logger) {
		try {
			this.logInfo = logger.get(getClass().getSimpleName(), LogLevel.INFO);
			this.logWarn = logger.get(getClass().getSimpleName(), LogLevel.WARN);
			this.logError = logger.get(getClass().getSimpleName(), LogLevel.CRITICAL);
			srnd = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public SlaveSpore allocateSession(String name, SessionEventListener listener) {
		while(true) {
			String uid = generateUID();
			SessionContext ctx = new SessionContext();
			ctx.listener = listener;			
			synchronized(ctx) {
				if (connections.putIfAbsent(uid, ctx) != null) {
					continue;
				}
				ctx.gateway = new RmiGateway(name);
				ctx.gateway.setStreamErrorHandler(ctx);
			}
			return new LegacySpore(uid);
		}		
	}
	
	@Override
	public AdvancedExecutor getExecutionService(String sessionId) {
		SessionContext ctx = connections.get(sessionId);
		if (ctx != null) {
			return ctx.gateway.getRemoteExecutorService();
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

	@Override
	public void dropAllSessions() {
		for(String id: connections.keySet()) {
			dropSession(id);
		}
	}
	
	@Override
	public void dropSession(String id) {
		SessionContext ctx = connections.get(id);
		if (ctx != null) {
			synchronized(ctx) {
				ctx = connections.get(id);
				if (ctx != null) {
					ctx.listener.closed();
					silentClose(ctx.stream);
					ctx.gateway.shutdown();
					connections.remove(id);
					
					// done
					return;
				}
			}
		}
		throw new IllegalArgumentException("Connection not found " + id);
	}
	
	@Override
	public void dispatch(DuplexStream stream) {
		String id = readId(stream);
		if (id != null) {
			SessionContext ctx = connections.get(id);
			if (ctx != null) {
				synchronized(ctx) {
					ctx = connections.get(id);
					if (ctx != null) {
						if (ctx.stream != null) {
							logWarn.log("New stream for " + id + " " + stream);
							logWarn.log("Old stream for " + id + " would be disposed " + ctx.stream);
							silentClose(ctx.stream);
							ctx.gateway.disconnect();
							if (ctx.stream != null) {
								ctx.listener.interrupted(ctx.stream);
								ctx.stream = null;
							}
						}
						try {
							ctx.gateway.connect(stream);
							ctx.stream = stream;
							ctx.listener.connected(stream);
						} catch (IOException e) {
							logError.log("Stream connection failed " + stream);
						}
						logInfo.log("Stream connected at end point " + id + " - " + stream);
						return;
					}
				}
			}
		}
		logWarn.log("Stream were not connected " + stream);
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
	
	private class SessionContext implements StreamErrorHandler {
		
		private SessionEventListener listener;
		private RmiGateway gateway;
		private DuplexStream stream;

		@Override
		public synchronized void streamError(DuplexStream socket, Object stream, Exception error) {
			gateway.disconnect();
			this.stream = null;
			listener.interrupted(socket);
		}

		@Override
		public void streamClosed(DuplexStream socket, Object stream) {
			gateway.disconnect();
			this.stream = null;
			logInfo.log("Closed: " + stream);
		}
	}
	
	private static final void silentClose(Closeable ch) {
		try {
			if (ch != null) {
				ch.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}
}
