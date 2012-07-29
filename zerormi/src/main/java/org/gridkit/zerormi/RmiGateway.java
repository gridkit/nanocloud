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
package org.gridkit.zerormi;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RmiGateway {

	private static final Logger LOGGER = LoggerFactory.getLogger(RmiGateway.class);
	
	private final RmiChannel channel;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	private boolean connected = false;
	private boolean terminated = false; 
	
	private DuplexStream socket;
	private RmiObjectInputStream in;
	private RmiObjectOutputStream out;

	private ExecutorService service;
	private CounterAgent remote;
	private Thread readerThread;
	
	private StreamErrorHandler streamErrorHandler = new StreamErrorHandler() {
		@Override
		public void streamError(DuplexStream socket, Object stream, Exception error) {
			shutdown();
		}
	};
	
	public RmiGateway() {
		this(new SmartRmiMarshaler());
	}

	public RmiGateway(RmiMarshaler marshaler) {
		// TODO should include counter agent
		this.channel = new RmiChannel(new MessageOut(), executor, marshaler);
		this.service = new RemoteExecutionService();
	}
	
	public ExecutorService getRemoteExecutorService() {
		return service;
	}
	
	public void setStreamErrorHandler(StreamErrorHandler errorHandler) {
		this.streamErrorHandler = errorHandler;
	}
	
	public void disconnect() {
		Thread readerThread = null;
		synchronized(this) {
			if (connected) {
				
				readerThread = this.readerThread;
				
				try {
					in.close();
				}
				catch(Exception e) {
					// ignore
				}
				try {
					out.close();
				}
				catch(Exception e) {
					// ignore
				}
				try {
					socket.close();
				}
				catch(Exception e) {
					// ignore
				}
				
				in = null;
				out = null;
				socket = null;
				connected = false;
			}
		}
		if (readerThread != null) {
			readerThread.interrupt();
			try {
				readerThread.join();
			} catch (InterruptedException e) {
				// ignore;
			}
		}
	}
	
	public synchronized boolean isConnected() {
		return connected && !terminated && !socket.isClosed();
	}
	
	public synchronized void shutdown() {
		if (terminated) {
			return;
		}
		terminated = true;
		try {
			out.close();
		}
		catch(Exception e) {
			// ignore
		}
		try {
			in.close();
		}
		catch(Exception e) {
			// ignore
		}
		try {
			service.shutdown();
		}
		catch(Exception e) {
			// ignore
		}
		try {
			channel.close();
		}
		catch(Exception e) {
			// ignore
		}
		try {
			executor.shutdown();		
		}
		catch(Exception e) {
			// ignore
		}
	}
	
	private final class SocketReader extends Thread {
		@Override
		public void run() {
			
			RmiObjectInputStream chin = in;
			try {
				while(true) {
					Object message = chin.readObject();
					if (message != null) {
						if ("close".equals(message)) {
							shutdown();
						}
						else {
							channel.handleRemoteMessage((RemoteMessage) message);
						}
					}
				}
			}
			catch(Exception e) {
				LOGGER.error("RMI stream read exception [" + socket + "]", e);
				DuplexStream socket = RmiGateway.this.socket;
				InputStream in = RmiGateway.this.in;
				readerThread = null;
				LOGGER.debug("disconnecting");
				disconnect();
				LOGGER.debug("streamError(...)");
				streamErrorHandler.streamError(socket, in, e);
			}
		}
	}

	public synchronized void connect(DuplexStream socket) throws IOException {
		if (this.socket != null) {
			throw new IllegalStateException("Already connected");
		}
		try {
			this.socket = socket;
			
			out = new RmiObjectOutputStream(socket.getOutput());
			
			CounterAgent localAgent = new LocalAgent();			
			channel.exportObject(CounterAgent.class, localAgent);
			synchronized(out) {					
				out.writeUnshared(localAgent);
				out.reset();
				out.flush();
			}
	
			// important create out stream first!
			in = new RmiObjectInputStream(socket.getInput());
			remote = (CounterAgent) in.readObject();
			
			readerThread = new SocketReader();
			readerThread.setName("RMI-Receiver: " + socket);
			readerThread.start();
			connected = true;			
			
		} catch (Exception e) {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e1) {
				// ignore
			}
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e1) {
				// ignore
			}
			try {
				if (this.socket != null) {
					this.socket.close();
				}
			}
			catch (Exception e1) {
				//ignore
			}
			in = null;
			out = null;
			this.socket = null;
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			else {
				throw new RuntimeException(e);
			}
		}
	}

	private class RmiObjectInputStream extends ObjectInputStream {
		
		public RmiObjectInputStream(InputStream in) throws IOException {
			super(in);
			enableResolveObject(true);
		}

		@Override
		protected Object resolveObject(Object obj) throws IOException {
			Object r = channel.streamResolveObject(obj);
			return r;
		}
	}

	private class RmiObjectOutputStream extends ObjectOutputStream {

		public RmiObjectOutputStream(OutputStream in) throws IOException {
			super(in);
			enableReplaceObject(true);
		}

		@Override
		protected Object replaceObject(Object obj) throws IOException {
			Object r = channel.streamReplaceObject(obj);
			return r;
		}
	}
	
	private class MessageOut implements RmiChannel.OutputChannel {
		public void send(RemoteMessage message) throws IOException {
			try {
				synchronized(out) {
					out.writeUnshared(message);
					out.reset();
				}
			} catch (IOException e) {
				DuplexStream socket = RmiGateway.this.socket;
				OutputStream out = RmiGateway.this.out;			
				disconnect();
				streamErrorHandler.streamError(socket, out, e);
				throw e;
			}
		}
	}
	
	public interface StreamErrorHandler {
		
		public void streamError(DuplexStream socket, Object stream, Exception error);
		
	}

	private class RemoteExecutionService extends AbstractExecutorService {
		
		private final ExecutorService threadPool = Executors.newCachedThreadPool();
		
		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			return submit(new CallableRunnableWrapper<T>(task, result));
		}

		@Override
		public Future<?> submit(Runnable task) {
			return submit(new CallableRunnableWrapper<Object>(task, null));
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			task = wrap(task);
			return threadPool.submit(task);
		}

		public void execute(Runnable command) {
			submit(new CallableRunnableWrapper<Object>(command, null));
		}

		private <T> Callable<T> wrap(final Callable<T> task) {
			return new Callable<T>() {
				public T call() throws Exception {
					return remote.remoteCall(task);
				}
			};
		}

		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			throw new UnsupportedOperationException();
		}

		public boolean isShutdown() {
			throw new UnsupportedOperationException();
		}

		public boolean isTerminated() {
			throw new UnsupportedOperationException();
		}

		public void shutdown() {
			RmiGateway.this.shutdown();
		}

		public List<Runnable> shutdownNow() {
			throw new UnsupportedOperationException();
		}
	}
	
	public static class CallableRunnableWrapper<T> implements Callable<T>, Serializable {

		private static final long serialVersionUID = 1L;

		private Runnable runnable;
		private T result;
		
		public CallableRunnableWrapper() {};
		
		public CallableRunnableWrapper(Runnable runnable, T result) {
			this.runnable = runnable;
			this.result = result;
		}

		public T call() throws Exception {
			runnable.run();
			return result;
		}
	}
	
	public static interface CounterAgent extends Remote {
		public <T> T remoteCall(Callable<T> callable) throws RemoteException, Exception;
	}
	
	private class LocalAgent implements CounterAgent {
		@Override
		public <T> T remoteCall(Callable<T> callable) throws Exception {
			return callable.call();
		}
	}
}
