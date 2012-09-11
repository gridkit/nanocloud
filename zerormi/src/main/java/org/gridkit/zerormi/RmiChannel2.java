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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.ComponentSuperviser.SuperviserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RmiChannel2 implements RmiInvocationHandler, RmiMarshaler {

	static boolean TRACE = false;
	
    private static final Logger LOGGER = LoggerFactory.getLogger(RmiChannel2.class);

    private static AtomicLong CALL_COUNTER = new AtomicLong(0L);

    private final String name;
    private final ComponentSuperviser superviser;
    private final ClassProvider classProvider;
    private final Executor defaultCallDispatcher;

    // runtime mappings
    
    private final Map<Object, RemoteInstance> object2remote = new IdentityHashMap<Object, RemoteInstance>();
    private final Map<RemoteInstance, Object> remote2object = new HashMap<RemoteInstance, Object>();

    private final Map<RemoteInstance, Object> remoteInstanceProxys = new ConcurrentHashMap<RemoteInstance, Object>();
    private final Map<Long, OutboundCallFuture> remoteReturnWaiters = new ConcurrentHashMap<Long, OutboundCallFuture>();

    private final Map<RemoteMethodSignature, Method> methodCache = new ConcurrentHashMap<RemoteMethodSignature, Method>();
    
    private DuplexObjectPipe pipe;

    private volatile boolean terminated = false;

    public RmiChannel2(String name, ComponentSuperviser superviser, ClassProvider provider, Executor callDispatcher) {
        this.name = name;
        this.superviser = superviser;
    	this.classProvider = provider;
        this.defaultCallDispatcher = callDispatcher;
    }

    public synchronized void setPipe(DuplexObjectPipe pipe) {
    	if (this.pipe == null) {
    		this.pipe = pipe;
    		pipe.bind(new PipeReceiver());
    	}
    	else {
    		throw new IllegalStateException("Pipe is bound already");
    	}    	
    }
    
    private synchronized void handleMessage(RemoteMessage message) {
    	
    	if (terminated) {
    		throw new IllegalStateException("Terminated [" + name +"]");
    	}
    	
        if (message instanceof RemoteCall) {

            final RemoteCall remoteCall = (RemoteCall) message;

            Runnable runnable = new InboundCallTask(remoteCall);

            defaultCallDispatcher.execute(runnable);

        } else if (message instanceof RemoteReturn) {
            RemoteReturn remoteReturn = (RemoteReturn) message;
            long id = remoteReturn.getCallId();
            OutboundCallFuture context;
            synchronized (this) {
				context = remoteReturnWaiters.remove(id);
			}
            if (context == null) {
                handleFatalInternalError(new RuntimeException("Orphaned remote return: " + remoteReturn));                
            }
            else {
            	context.setRemoteResult(remoteReturn);
            }
        } else {
        	handleFatalInternalError(new RuntimeException("Unknown RemoteMessage type. " + message));
        }
    }

    public synchronized void destroy() {

    	synchronized(this) {
	    	if (terminated) {
	            return;
	        }
	        
	    	terminated = true;
	
	        object2remote.clear();
	        remote2object.clear();
	
	        remoteInstanceProxys.clear();
	        for (OutboundCallFuture context : remoteReturnWaiters.values()) {
	        	try {
	        		context.setError(newChannelClosedExceptions());
	        	}
	        	catch(Exception e) {
	        		// ignore
	        	}
	        }
	        remoteReturnWaiters.clear();
	        pipe.close();
    	}
    }

	private RemoteException newChannelClosedExceptions() {
		return new RemoteException("Channel [" + name + "] has been closed");
	}

    private void handleFatalInternalError(Throwable e) {    	
		superviser.onFatalError(SuperviserEvent.newUnexpectedError(this, e));
        destroy();
	}

	private FutureEx<Void> sendMessage(RemoteMessage message) {
		if (message != null) {
			
			DuplexObjectPipe out;
			synchronized(this) {
				out = terminated ? null : pipe;
			}
			if (out != null) {
				if (TRACE) {
					System.out.println(name + ": out -> " + message);
				}
				return pipe.sendObject(message);
			}
		}
		FutureBox<Void> box = new FutureBox<Void>();
		box.setError(newChannelClosedExceptions());
		return box;
    }

    private RemoteReturn delegateCall(RemoteCall remoteCall) {

        Object target = remoteCall.getTarget();
        RemoteMethodSignature methodId = remoteCall.getMethod();
        long callId = remoteCall.getCallId();

        RemoteReturn remoteReturn;

        Method implementationMethod;
        try {
            implementationMethod = lookupMethod(methodId);
        } catch (Exception e) {
            return new RemoteReturn(true, new RemoteException(String.format("Method cannot be resolved ", methodId)), callId);
        }

        Object methodReturn = null;
        try {
            methodReturn = implementationMethod.invoke(target, remoteCall.getArgs());
            remoteReturn = new RemoteReturn(false, methodReturn, callId);
        } catch (InvocationTargetException e) {
            remoteReturn = new RemoteReturn(true, e.getCause(), callId);
        } catch (Exception e) {
            remoteReturn = new RemoteReturn(true, new RemoteException("Invocation failed", e), callId);
        }

        return remoteReturn;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Method lookupMethod(RemoteMethodSignature methodSig) throws ClassNotFoundException, SecurityException, NoSuchMethodException {

        Method method = methodCache.get(methodSig);
        if (method != null) {
            return method;
        } else {
            Class iface = classProvider.classForName(methodSig.getClassName());
            String methodName = methodSig.getMethodName();
            Class[] argTypes = toClassObjects(methodSig.getMethodSignature());
            method = iface.getMethod(methodName, argTypes);
            method.setAccessible(true);
            methodCache.put(methodSig, method);
            return method;
        }
    }

    private Long generateCallId() {
    	Long id = CALL_COUNTER.getAndIncrement();
    	if (remoteReturnWaiters.containsKey(id)) {
    		return generateCallId();
    	}
    	else {
    		return id;
    	}
    }
    
    public boolean isRemoteProxy(Object proxy) {
    	try {
	    	if (Proxy.isProxyClass(proxy.getClass())) {
	    		InvocationHandler ih = Proxy.getInvocationHandler(proxy);
	    		if (ih instanceof RemoteStub) {
	    			return ((RemoteStub)ih).getRmiChannel() == this;
	    		}
	    	}
    	}
    	catch(IllegalArgumentException e) {
    		// ignore
    	}
    	return false;
    }
    
    @SuppressWarnings("unchecked")
	public <V> FutureEx<V> invokeRemotely(Object target, Method m, Object... args) {
    	if (target == null) {
    		return FutureBox.errorFuture(new NullPointerException());
    	}
    	try {
    		verifyMethod(target, m);
    	}
    	catch(Exception e) {
    		return FutureBox.errorFuture(new NullPointerException());
    	}
//    	if (isRemoteProxy(target)) {
//    		RemoteInstance ri = ((RemoteStub)Proxy.getInvocationHandler(target)).getRemoteInstance();
//    		target = new RemoteRef(ri);
//    	}
		return (FutureEx<V>)asyncInvoke(target, m, args);
    }
    
    private void verifyMethod(Object target, Method m) throws SecurityException, NoSuchMethodException {
    	// unsure we are calling public method
		target.getClass().getMethod(m.getName(), m.getParameterTypes());    	
	}

	protected OutboundCallFuture asyncInvoke(final Object target, final Method method, Object[] args) {
    	Long id = generateCallId();
    	RemoteCall remoteCall = new RemoteCall(target, new RemoteMethodSignature(method), args, id);
    	OutboundCallFuture future = new OutboundCallFuture(remoteCall);
    	
    	registerCall(future);
    	
    	return future;
    }
    
    private synchronized void registerCall(OutboundCallFuture future) {
    	if (terminated) {
    		future.setError(newChannelClosedExceptions());
    	}
    	else {
    		final long callId = future.remoteCall.getCallId();
    		try {
    			remoteReturnWaiters.put(future.remoteCall.getCallId(), future);
				final FutureEx<Void> sendFuture = sendMessage(future.remoteCall);
				sendFuture.addListener(new Box<Void>() {

					@Override
					public void setData(Void data) {
						// do nothing
					}

					@Override
					public void setError(Throwable e) {
						abortCall(callId, e);
					}
				});
			} catch (Exception e) {
				abortCall(callId, e);
				return;
			}
    	}
	}

    private synchronized void abortCall(long callId, Throwable e) {
    	OutboundCallFuture future = remoteReturnWaiters.remove(callId);
    	if (future == null) {
    		superviser.onFatalError(SuperviserEvent.newUnexpectedError(this, "Call future is missing " + callId));
    	}
    	future.setError(e);
    }
    
    private Object getProxyFromRemoteInstance(RemoteInstance remoteInstance) throws IOException {
        Object proxy = remoteInstanceProxys.get(remoteInstance);
        if (proxy == null) {
            try {
                proxy = buildProxy(remoteInstance);
            } catch (ClassNotFoundException e) {
                throw new IOException("Cannot create proxy for remote instance", e);
            }
            remoteInstanceProxys.put(remoteInstance, proxy);
            remote2object.put(remoteInstance, proxy);
            object2remote.put(proxy, remoteInstance);
        }
        return proxy;
    }
    
	@SuppressWarnings("rawtypes")
	private Object buildProxy(RemoteInstance remoteInstance) throws ClassNotFoundException {
		String[] classNames = remoteInstance.interfaces;
		Class[] classes = new Class[classNames.length];
		for(int i = 0; i != classNames.length; ++i) {
			classes[i] = classProvider.classForName(classNames[i]);
		}
		
		return classProvider.newProxyInstance(classes, new RemoteStub(remoteInstance, this));
	}


    public void exportObject(Class<?> iface, Object implementation) {
        internalExportObject(new Class[]{iface}, implementation);
    }

    public void exportObject(Class<?>[] iface, Object implementation) {
    	internalExportObject(Arrays.copyOf(iface, iface.length), implementation);
    }

    @SuppressWarnings({ "rawtypes" })
    private synchronized RemoteInstance internalExportObject(Class[] interfaces, Object obj) {
        RemoteInstance remote = object2remote.get(obj);
        if (remote == null) {
            String uuid = UUID.randomUUID().toString();
            String[] ifNames = new String[interfaces.length];
            for (int i = 0; i != ifNames.length; ++i) {
                ifNames[i] = interfaces[i].getName();
            }
            remote = new RemoteInstance(uuid, ifNames);
            object2remote.put(obj, remote);
            remote2object.put(remote, obj);
        }
        // TODO verify exported interfaces
        return remote;
    }

    @Override
	public Object writeReplace(Object obj) throws IOException {
        // allow explicit export
        synchronized (this) {
            RemoteInstance id = object2remote.get(obj);
            if (id != null) {
                return new RemoteRef(id);
            }
        }

        if (obj instanceof Exported) {
        	Exported exp = (Exported) obj;
        	return new RemoteRef(internalExportObject(exp.getInterfaces(), exp.getObject()));
        }
        else {
        	return obj; 
        }
	}

	@Override
	public Object readResolve(Object obj) throws IOException {
        if (obj instanceof RemoteRef) {
        	RemoteRef ref = (RemoteRef)obj;
        	synchronized (this) {
        		Object local = remote2object.get(ref.getIdentity());
        		if (local != null) {
        			return local;
        		}
        		else {
        			return getProxyFromRemoteInstance(((RemoteRef) obj).getIdentity());
        		}
        	}
        }
        else {
        	return obj;
        }
	}

    @SuppressWarnings({ "rawtypes" })
    private Class[] toClassObjects(String[] names) throws ClassNotFoundException {
        Class[] classes = new Class[names.length];
        for (int i = 0; i != names.length; ++i) {
            classes[i] = classProvider.classForName(names[i]);
        }
        return classes;
    }

    private final class InboundCallTask implements Runnable {
		
    	private final RemoteCall remoteCall;

		private InboundCallTask(RemoteCall remoteCall) {
			this.remoteCall = remoteCall;
		}

		public void run() {
//		    String threadName = Thread.currentThread().getName();
//		    Thread.currentThread().setName("RMI-CALL[" + remoteCall.toShortString() + "]");

		    try {
		        RemoteReturn remoteReturn;
		        try {
		            remoteReturn = delegateCall(remoteCall);
		        } catch (Exception e) {
		        	handleFatalInternalError(e);
		            return;
		        }
		        try {
		        	final long callId = remoteReturn.callId;
		            sendMessage(remoteReturn).addListener(new Box<Void>() {
		            	@Override
		            	public void setData(Void data) {
		            		// do nothing
		            	}

		            	@Override
						public void setError(Throwable e) {
		            		if (e instanceof RemoteException) {
		            			// assume channel closed exception
		            			// ignore
		            		}
		            		else {
			            		LOGGER.warn("Failed to send call result: " + e.toString());
			            		Exception ee = new IOException("Failed to marshal call results: " + e.toString());
								RemoteReturn errorRet = new RemoteReturn(true, ee, callId);
								sendMessage(errorRet).addListener(new Box<Void>() {

									@Override
									public void setData(Void data) {
										// ok, do nothing
									}

									@Override
									public void setError(Throwable e) {
										handleFatalInternalError(e);
									}
								});
		            		}
						}
					});
		        } catch (Exception e) {
		        	handleFatalInternalError(e);
		        }
		    }
		    finally {
//		        Thread.currentThread().setName(threadName);
		    }
		}
		
		public String toString() {
			return "RMI-CALL[" + remoteCall.toShortString() + "]";			
		}
	}

    class PipeReceiver implements DuplexObjectPipe.ObjectReceiver {

		@Override
		public FutureEx<Void> objectReceived(Object object) {
			if (TRACE) {
				System.out.println(name + ":  in <- " + object);
			}
			handleMessage((RemoteMessage) object);
			return FutureBox.dataFuture(null);
		}

		@Override
		public void closed() {
			destroy();
		}    	
    }
    
    private static class OutboundCallFuture extends FutureBox<Object> {

    	RemoteCall remoteCall;

    	public OutboundCallFuture(RemoteCall remoteCall) {
    		this.remoteCall = remoteCall;
		}

		public void setRemoteResult(RemoteReturn remoteReturn) {
			if (remoteReturn.throwing) {
				Throwable e = (Throwable) remoteReturn.ret;
				setError(e);
			}
			else {
				setData(remoteReturn.ret);
			}			
		}
    }
}
