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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RmiChannel2 implements RmiChannel, RmiMarshaler {

    private static final Logger logger = LoggerFactory.getLogger(RmiChannel2.class);

    private static AtomicLong callId = new AtomicLong(0L);

    private final String name;
    private final OutputChannel messageOut;
    private final Executor callDispatcher;

    private final Map<Object, RemoteInstance> object2remote = new IdentityHashMap<Object, RemoteInstance>();
    private final Map<RemoteInstance, Object> remote2object = new HashMap<RemoteInstance, Object>();

    private final Map<RemoteInstance, Object> remoteInstanceProxys = new ConcurrentHashMap<RemoteInstance, Object>();
    private final Map<Long, RemoteCallFuture> remoteReturnWaiters = new ConcurrentHashMap<Long, RemoteCallFuture>();

    private final Map<RemoteMethodSignature, Method> methodCache = new ConcurrentHashMap<RemoteMethodSignature, Method>();

    private volatile boolean terminated = false;

    public RmiChannel2(String name, OutputChannel output, Executor callDispatcher) {
        this.name = name;
    	this.messageOut = output;
        this.callDispatcher = callDispatcher;
    }

    //public 
    
    public void handleMessage(RemoteMessage message) {
        if (message instanceof RemoteCall) {

            final RemoteCall remoteCall = (RemoteCall) message;

            Runnable runnable = new Runnable() {
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    Thread.currentThread().setName("RMI-CALL[" + remoteCall.toShortString() + "]");

                    try {
                        RemoteReturn remoteReturn;
                        try {
                            remoteReturn = delegateCall(remoteCall);
                        } catch (Exception e) {
                        	handleFatalInternalError(e);
                            return;
                        }
                        try {
                            sendMessage(remoteReturn);
                        } catch (IOException e) {
                        	handleFatalInternalError(e);
                        }
                    }
                    finally {
                        Thread.currentThread().setName(threadName);
                    }
                }
            };

            callDispatcher.execute(runnable);

        } else if (message instanceof RemoteReturn) {
            RemoteReturn remoteReturn = (RemoteReturn) message;
            long id = remoteReturn.getCallId();
            RemoteCallFuture context;
            synchronized (this) {
				context = remoteReturnWaiters.remove(id);
			}
            if (context == null) {
                handleFatalInternalError(new RuntimeException("Orphaned remote return: " + remoteReturn));
            }
            context.setRemoteResult(remoteReturn);
        } else {
        	handleFatalInternalError(new RuntimeException("Unknown RemoteMessage type. " + message));
        }
    }

    public synchronized void close() {
        // TODO global synchronization somehow
        if (terminated) {
            return;
        }
        terminated = true;

        object2remote.clear();
        remote2object.clear();

        remoteInstanceProxys.clear();
        for (RemoteCallFuture context : remoteReturnWaiters.values()) {
        	try {
        		context.setError(newChannelClosedExceptions());
        	}
        	catch(Exception e) {
        		// ignore
        	}
        }
        remoteReturnWaiters.clear();
    }

	private RemoteException newChannelClosedExceptions() {
		return new RemoteException("Channel [" + name + "] has been closed");
	}

    private void handleFatalInternalError(Exception e) {
    	// TODO
        e.printStackTrace();
        close();
	}

	protected void sendMessage(RemoteMessage message) throws IOException {
        messageOut.send(message);
    }

    protected RemoteReturn delegateCall(RemoteCall remoteCall) {

        RemoteInstance instance = remoteCall.getRemoteInstance();
        RemoteMethodSignature methodId = remoteCall.getMethod();
        long callId = remoteCall.getCallId();

        Object implementator;
        synchronized (this) {
            implementator = remote2object.get(remoteCall.getRemoteInstance());
        }

        if (implementator == null) {
            return new RemoteReturn(true, new RemoteException(String.format("Instance %s has not been exported ", instance)), callId);
        }

        RemoteReturn remoteReturn;

        Method implementationMethod;
        try {
            implementationMethod = lookupMethod(methodId);
        } catch (Exception e) {
            return new RemoteReturn(true, new RemoteException(String.format("Method cannot be resolved ", methodId)), callId);
        }

        Object methodReturn = null;
        try {
            methodReturn = implementationMethod.invoke(implementator, remoteCall.getArgs());
            remoteReturn = new RemoteReturn(false, methodReturn, callId);
        } catch (InvocationTargetException e) {
            System.err.println("Call[" + remoteCall.toShortString() + "] exception " + e.getCause().toString());
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
            Class iface = classForName(methodSig.getClassName());
            String methodName = methodSig.getMethodName();
            Class[] argTypes = toClassObjects(methodSig.getMethodSignature());
            method = iface.getMethod(methodName, argTypes);
            method.setAccessible(true);
            methodCache.put(methodSig, method);
            return method;
        }
    }

    public Long generateCallId() {
    	Long id = callId.getAndIncrement();
    	if (remoteReturnWaiters.containsKey(id)) {
    		return generateCallId();
    	}
    	else {
    		return id;
    	}
    }
    
    public RemoteCallFuture asyncInvoke(final RemoteInstance remoteInstance, final Method method, Object[] args) throws Throwable {
    	Long id = generateCallId();
    	RemoteCall remoteCall = new RemoteCall(remoteInstance, new RemoteMethodSignature(method), args, id);
    	RemoteCallFuture future = new RemoteCallFuture(remoteCall);
    	
    	registerCall(future);
    	
    	return future;
    }
    
    private synchronized void registerCall(RemoteCallFuture future) {
    	if (terminated) {
    		future.setError(newChannelClosedExceptions());
    	}
    	else {
    		try {
				sendMessage(future.remoteCall);
			} catch (Exception e) {
				future.setError(new RemoteException("Remote call failed", e));
				return;
			}
    		remoteReturnWaiters.put(future.remoteCall.getCallId(), future);
    	}
	}

    @Override
	public Object remoteInvocation(final RemoteStub stub, final Object proxy, final Method method, final Object[] args) throws Throwable {
    	
    	Future<Object> call = asyncInvoke(stub.getRemoteInstance(), method, args);
    	try {
    		return call.get();
    	}
    	catch(ExecutionException e) {
    		throw e.getCause();
    	}
    }

    private Object getProxyFromRemoteInstance(RemoteInstance remoteInstance) {
        Object proxy = remoteInstanceProxys.get(remoteInstance);
        if (proxy == null) {
            try {
                proxy = RemoteStub.buildProxy(remoteInstance, this);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            remoteInstanceProxys.put(remoteInstance, proxy);
        }
        return proxy;
    }

    public <T> void exportObject(Class<T> iface, T implementation) {
        exportObject(new Class[]{iface}, implementation);
    }

    @SuppressWarnings({ "rawtypes" })
    private RemoteInstance exportObject(Class[] interfaces, Object obj) {
        synchronized (object2remote) {
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
            return remote;
        }
    }

    public Object streamResolveObject(Object obj) throws IOException {
    	
    	if (obj == null) {
    		return null;
    	}
    	
    	if (obj instanceof BeanRef) {
            BeanRef ref = (BeanRef) obj;
            Object bean = name2bean.get(ref.getBeanName());
            if (bean == null) {
                logger.warn("Cannot resolve bean named '" + ref + "'");
            }
            return bean;
        }
        if (obj instanceof RemoteRef) {
            return getProxyFromRemoteInstance(((RemoteRef) obj).getIdentity());
        } else {
            return marshaler.readResolve(obj);
        }
    }

    public Object streamReplaceObject(Object obj) throws IOException {
    	
    	if (obj == null) {
    		return null;
    	}
    	
        if (bean2name.containsKey(obj)) {
            return new BeanRef(bean2name.get(obj));
        }

        // allow explicit export
        synchronized (object2remote) {
            RemoteInstance id = object2remote.get(obj);
            if (id != null) {
                return new RemoteRef(id);
            }
        }

        Object mr = marshaler.writeReplace(obj);
        if (mr instanceof Exported) {
        	Exported exp = (Exported) mr;
        	return new RemoteRef(exportObject(exp.getInterfaces(), exp.getObject()));
        }
        
        return mr;
    }

    @SuppressWarnings("rawtypes")
	public static String[] toClassNames(Class[] classes) {
        String[] names = new String[classes.length];
        for (int i = 0; i != classes.length; ++i) {
            names[i] = classes[i].getName();
        }
        return names;
    }

    @SuppressWarnings({ "rawtypes" })
    public Class[] toClassObjects(String[] names) throws ClassNotFoundException {
        Class[] classes = new Class[names.length];
        for (int i = 0; i != names.length; ++i) {
            classes[i] = classForName(names[i]);
        }
        return classes;
    }

    @SuppressWarnings({ "rawtypes" })
    public Class classForName(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    public interface OutputChannel {
        public void send(RemoteMessage message) throws IOException;
    }

    private static class RemoteCallContext {
        public final Thread thread;
        public RemoteReturn result;

        public RemoteCallContext(Thread thread) {
            this.thread = thread;
        }
    }
    
    private static class RemoteCallFuture extends FutureBox<Object> {

    	RemoteCall remoteCall;

    	public RemoteCallFuture(RemoteCall remoteCall) {
    		this.remoteCall = remoteCall;
		}

		public void setRemoteResult(RemoteReturn remoteReturn) {
			if (remoteReturn.throwing) {
				Throwable e = (Throwable) remoteReturn.ret;
				if (e instanceof Exception) {
					setError((Exception) e);
				}
				else {
					setError(new UndeclaredThrowableException(e));
				}
			}
			else {
				setData(remoteReturn.ret);
			}			
		}
    }
}
