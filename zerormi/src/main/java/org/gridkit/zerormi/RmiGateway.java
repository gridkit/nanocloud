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

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutorAdapter;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.LogStream;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.gridkit.zerormi.zlog.ZLogger;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RmiGateway {

	private final RmiChannel channel;
	private final ExecutorService executor;
	
	private boolean connected = false;
	private boolean terminated = false; 
	
	private String name;
	private DuplexStream socket;
	private InboundMessageStream in;
	private OutboundMessageStream out;

	private RemoteExecutionService service;
	private CounterAgent remote;
	private Thread readerThread;
	
	private final LogStream logVerbose;
	private final LogStream logInfo;
	private final LogStream logCritical;
	
	private StreamErrorHandler streamErrorHandler = new StreamErrorHandler() {
		@Override
		public void streamError(DuplexStream socket, Object stream, Exception error) {
			shutdown();
		}

		@Override
		public void streamClosed(DuplexStream socket, Object stream) {
			shutdown();
		}
	};
	
	public RmiGateway(String name) {
		this(name, new SmartRmiMarshaler(), ZLogFactory.getDefaultRootLogger().getLogger(RmiGateway.class.getPackage().getName()), Collections.<String, Object>emptyMap());
	}

	public RmiGateway(String name, ZLogger logger) {
		this(name, new SmartRmiMarshaler(), logger, Collections.<String, Object>emptyMap());
	}

	private ExecutorService createRmiExecutor() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                100, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {
					int counter = 1;
					
					@Override
					public synchronized Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("RMI[" + name + "]-worker-" + (counter++));
						t.setDaemon(true);
						return t;
					}
				});
	}

	public RmiGateway(String name, RmiMarshaler marshaler, ZLogger logger, Map<String, Object> props) {
		// TODO should include counter agent
		this.executor = createRmiExecutor();
		this.channel = new RmiChannel1(name, new MessageOut(), executor, marshaler, logger, props);
		this.service = new RemoteExecutionService();
		this.name = name;
		this.logVerbose = logger.get(getClass().getSimpleName(), LogLevel.VERBOSE);
		this.logInfo = logger.get(getClass().getSimpleName(), LogLevel.INFO);
		this.logCritical = logger.get(getClass().getSimpleName(), LogLevel.CRITICAL);
	}
	
	public AdvancedExecutor getRemoteExecutorService() {
		return service;
	}
	
	public void setStreamErrorHandler(StreamErrorHandler errorHandler) {
		this.streamErrorHandler = errorHandler;
	}
	
	public void disconnect() {
		Thread readerThread = null;
		synchronized(this) {
			if (connected) {
				
				logInfo.log("RMI gateway [" + name +"] disconneted.");
				
				readerThread = this.readerThread;
				
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
		logInfo.log("RMI gateway [" + name +"] terminated.");
		terminated = true;
		
		try {
			out.close();
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
			in.close();
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
	
	private final class SocketReader extends Thread implements Closeable {
		
		@Override
		public void interrupt() {
			super.interrupt();
			close();			
		}

		// needed for Isolate shutdown support
		@Override
		public void close() {
			if (in != null) {
				in.close();
			}
			try {
				if (socket != null) {
					socket.close();
				}
			}
			catch (IOException e) {
				// ignore
			}
		}

		@Override
		public void run() {
			
			InboundMessageStream ims = in;
			try {
				while(!terminated) {
					RemoteMessage message = ims.readMessage();
					if (message == null) {
						logInfo.log("RMI gateway [" + name + "], remote side has requested termination");
						shutdown();
						return;
					}
					else {
						channel.handleMessage(message);
					}
				}
			}
			catch(Exception e) {
				if (IOHelper.isSocketTerminationException(e)) {
					logVerbose.log("RMI stream, socket has been discontinued [" + socket + "] - " + e.toString());
				}
				else {
					logCritical.log("RMI stream read exception [" + socket + "]", e);
				}
				DuplexStream socket = RmiGateway.this.socket;
				InputStream in = RmiGateway.this.in.tstream;
				readerThread = null;
				logVerbose.log("disconnecting");
				disconnect();
				if (IOHelper.isSocketTerminationException(e)) {
					streamErrorHandler.streamClosed(socket, in);
				}
				else {
					streamErrorHandler.streamError(socket, in, e);
				}
			}
		}
	}

	public synchronized void connect(DuplexStream socket) throws IOException {
		if (this.socket != null) {
			throw new IllegalStateException("Already connected");
		}
		try {
			this.socket = socket;
			
			out = new OutboundMessageStream(socket.getOutput());
			
			CounterAgent localAgent = new LocalAgent();			
			channel.exportObject(CounterAgent.class, localAgent);
			out.writeHandShake(localAgent);
	
			// important create out stream first!
			in = new InboundMessageStream(socket.getInput());
			remote = (CounterAgent) in.readHandShake();
			
			readerThread = new SocketReader();
			readerThread.setName("RMI-Receiver: " + socket);
			readerThread.start();
			connected = true;			
			
		} catch (Exception e) {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
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

	static long TAG_CALL = 1;
	static long TAG_RETURN = 2;
	static long TAG_THROW = 3;
	static long TRAILER_SUCCESS = 10;
	static long TRAILER_DISCARD = 20;
	static long TRAILER_ERROR = 30;
	
	static byte[] canary = new byte[0];
	
	private class InboundMessageStream {
	    
	    byte[] callId = new byte[7];
	    InputStream tstream;
	    EnvelopInputStream estream; 
	    DataInputStream dstream;
	    RmiObjectInputStream ostream;
	    
	    public InboundMessageStream(InputStream stream) throws IOException {
	        this.tstream = stream;
	        this.estream = new EnvelopInputStream(tstream);
	        this.dstream = new DataInputStream(estream);
	        this.ostream = new RmiObjectInputStream(estream);
	    }
	    
	    public void close() {
            try {
                tstream.close();
            } catch (IOException e) {
                // ignore
            }
        }

	    public Object readHandShake() throws IOException, ClassNotFoundException {
	        Object obj = ostream.readObject();
	        ostream.readObject(); // null expected
	        estream.nextMessage();
	        return obj;
	    }
	    
        public RemoteMessage readMessage() throws IOException {
	        while(true) {
    	        int tag = estream.read();
    	        if (tag == -1) {
    	            return null; // End of Stream
    	        }
    	        else if (tag == TRAILER_SUCCESS) {
    	            // ignore
    	            estream.nextMessage();
    	            return readMessage();
    	        }
    	        else if (tag == TRAILER_DISCARD) {
    	            // ignore
    	            estream.nextMessage();
    	            return readMessage();
    	        }
    	        else if (tag == TAG_CALL) {
    	            long callId = readCallId();
    	            RemoteMessage msg;
    	            RemoteInstance ri = null;
    	            RemoteMethodSignature m = null;
    	            Object[] args = null;
    	            try {
                        ri = (RemoteInstance) ostream.readObject();
                        m = (RemoteMethodSignature) ostream.readObject();
                        args = (Object[]) ostream.readObject();
                        ostream.readObject(); // null expected
                        
                        msg = new RemoteCall(callId, ri, m, args);
                        estream.nextMessage();
                    } catch (NoClassDefFoundError e) {
                        recover();
                        msg = processFollowUp(new InboundCallError(callId, ri, m, new RemoteException("Unparsable call", e)));
                        if (msg == null) {
                            continue;
                        }
                    } catch (Exception e) {
        	            recover();
                        msg = processFollowUp(new InboundCallError(callId, ri, m, new RemoteException("Unparsable call", e)));
                        if (msg == null) {
                            continue;
                        }
        	        }
    	            return msg;
    	        }
    	        else if (tag == TAG_RETURN || tag == TAG_THROW) {
                    long callId = readCallId();
                    RemoteMessage msg;
                    try {
                        Object obj = ostream.readObject();
                        ostream.readObject(); // null expected
                        if (tag == TAG_RETURN) {
                            msg = new RemoteReturn(callId, false, obj);
                        }
                        else {
                            msg = new RemoteReturn(callId, true, obj);
                        }
                        estream.nextMessage();
                    } catch (NoClassDefFoundError e) {
                        recover();
                        msg = new RemoteReturn(callId, true, new RemoteException("Unparsable result", e));
                        msg = processFollowUp(msg);
                        if (msg == null) {
                            continue;
                        }
                    } catch (Exception e) {
                        recover();
                        msg = new RemoteReturn(callId, true, new RemoteException("Unparsable result", e));
                        msg = processFollowUp(msg);
                        if (msg == null) {
                            continue;
                        }
                    }
                    return msg;	            
    	        }
    	        else {
    	            throw new IOException("Stream corrupted, unknown tag: " + tag);
    	        }
	        }
	    }

        private RemoteMessage processFollowUp(RemoteMessage lastError) throws IOException {
            int tag = estream.read();
            if (tag < 0) {
                estream.nextMessage();               
                return lastError;
            }
            else if (tag == TRAILER_SUCCESS) {
                estream.nextMessage();               
                return lastError;                
            }
            else if (tag == TRAILER_DISCARD) {
                estream.nextMessage();               
                return null;                
            }
            else if (tag == TRAILER_ERROR) {
                RemoteMessage msg = lastError;
                try {
                    readCallId();
                    Object error = ostream.readObject();
                    ostream.readObject(); // null expected
                    msg = new RemoteReturn(lastError.getCallId(), true, error);
                    estream.nextMessage();
                }
                catch(NoClassDefFoundError e) {
                    recover();
                }
                catch(Exception e) {
                    recover();
                }
                return msg;
            }
            else {
                throw new IOException("Stream corrupted, unknown tag: " + tag);
            }
        }

        private void recover() throws IOException {
            estream.skip(Long.MAX_VALUE);
            estream.nextMessage();
            ostream = new RmiObjectInputStream(estream);
            
        }

        private long readCallId() throws IOException {
            dstream.readFully(callId, 0, callId.length);
            return ((long)(callId[0] & 255) << 48) +
                    ((long)(callId[1] & 255) << 40) +
                    ((long)(callId[2] & 255) << 32) +
                    ((long)(callId[3] & 255) << 24) +
                    ((callId[4] & 255) << 16) +
                    ((callId[5] & 255) <<  8) +
                    ((callId[6] & 255) <<  0);
        }
	}

	private class OutboundMessageStream {

        OutputStream tstream;
        EnvelopOutputStream estream;
        DataOutputStream dstream;
        RmiObjectOutputStream ostream;
        
        public OutboundMessageStream(OutputStream stream) throws IOException {
            this.tstream = stream;
            this.estream = new EnvelopOutputStream(tstream);
            this.dstream = new DataOutputStream(estream);
            this.ostream = new RmiObjectOutputStream(estream);
        }
        
        public void close() {
            try {
                tstream.close();
            } catch (IOException e) {
                // ignore
            }
        }

        public void writeHandShake(Object object) throws IOException {
            ostream.writeObject(object);
            ostream.reset();
            ostream.writeObject(null); // we need this to ensure reset is processed by read side
            ostream.flush();
            estream.closeMessage();
        }
        
        public void writeMessage(RemoteCall call) throws IOException {
//            System.out.println("[OUT:" + estream.hashCode() + "] remote call");
            long id = call.getCallId();
            id |= ((long)TAG_CALL) << 56;
            dstream.writeLong(id);
            try {
                ostream.writeObject(call.getRemoteInstance());
                ostream.writeObject(call.getMethod());
                ostream.writeObject(call.getArgs());
                ostream.reset();
                ostream.writeObject(null);
                ostream.flush();
                // success
                estream.closeMessage();
                // writing empty trailer
                dstream.writeLong(TRAILER_SUCCESS << 56);
                estream.closeMessage();
            }
            catch(Exception e) {
                recover();
                discard();
                throw new RecoverableSerializationException(e);
            }
        }

        public void writeMessage(RemoteReturn result) throws IOException {
//            System.out.println("[OUT:" + estream.hashCode() + "] remote return");
            long id = result.getCallId();
            if (result.isThrowing()) {
                id |= ((long)TAG_THROW) << 56;
            }
            else {
                id |= ((long)TAG_RETURN) << 56;
            }
            dstream.writeLong(id);
            try {
                ostream.writeObject(result.getRet());
                ostream.reset();
                ostream.writeObject(null);
                ostream.flush();
                // success
                estream.closeMessage();
                // writing empty trailer
                dstream.writeLong(TRAILER_SUCCESS << 56);
                estream.closeMessage();
            }
            catch(Exception e) {
                recover();
                followUp(result.callId, e);
            }            
        }
        
        private void followUp(long callId, Exception e) throws IOException {
//            System.out.println("[OUT:" + estream.hashCode() + "] follow up");
            long id = callId ;
            id |= ((long)TRAILER_ERROR) << 56;
            dstream.writeLong(id);
            try {
                ostream.writeObject(new RemoteException("Unwritable result", e));
                ostream.reset();
                ostream.writeObject(null);
                ostream.flush();
                // success
                estream.closeMessage();
            }
            catch(Exception ee) {
                recover();
            }            
        }

        private void discard() throws IOException {
//            System.out.println("[OUT:" + estream.hashCode() + "] discard");
            dstream.writeLong((long)TRAILER_DISCARD << 56);
            estream.closeMessage();
        }

        private void recover() throws IOException {
            estream.closeMessage();
            ostream = new RmiObjectOutputStream(estream);            
        }	    
	}
	
	private class RmiObjectInputStream extends ObjectInputStream {
		
		public RmiObjectInputStream(InputStream in) throws IOException {
			super(in);
			enableResolveObject(true);
		}
		
		@Override
        protected void readStreamHeader() throws IOException, StreamCorruptedException {
		    // suppress stream header,
		    // so stream can be reused until failure
        }

        @Override
		protected Object resolveObject(Object obj) throws IOException {
			Object r = channel.streamResolveObject(obj);
			return r;
		}

		@Override
		public String toString() {
			return "RmiObjectInputStream[" + name + "]";
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

        @Override
        protected void writeStreamHeader() throws IOException {
            // suppress stream header,
            // so stream can be reused until failure
        }
	}

	private class MessageOut implements RmiChannel1.OutputChannel {
		public void send(RemoteMessage message) throws IOException {
			try {
				synchronized(out) {
				    if (message instanceof RemoteCall) {
				        out.writeMessage((RemoteCall)message);
				    }
				    else {
				        out.writeMessage((RemoteReturn)message);
				    }
				}
			}
			catch (NullPointerException e) {
				if (out == null) {
					throw new IOException("RMI gatway [" + name + "] channel is not connected");
				}
				else throw e;
			}
			catch (IOException e) {
				DuplexStream socket = RmiGateway.this.socket;
				OutputStream out = RmiGateway.this.out.tstream;			
				disconnect();
				streamErrorHandler.streamError(socket, out, e);
				throw e;
			}
		}
	}
	
	public interface StreamErrorHandler {
		
		public void streamError(DuplexStream socket, Object stream, Exception error);
		
		public void streamClosed(DuplexStream socket, Object stream);
		
	}

	private class RemoteExecutionService extends AbstractExecutorService implements AdvancedExecutor {
		
		private final ExecutorService threadPool = executor;
		private final AdvancedExecutorAdapter adapter = new AdvancedExecutorAdapter(threadPool);
		
		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			return submit(new CallableRunnableWrapper<T>(task, result));
		}

		@Override
		public FutureEx<Void> submit(Runnable task) {
			return submit(new CallableRunnableWrapper<Void>(task, null));
		}

		@Override
		public <T> FutureEx<T> submit(Callable<T> task) {
			task = wrap(task);
			return adapter.submit(task);
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
