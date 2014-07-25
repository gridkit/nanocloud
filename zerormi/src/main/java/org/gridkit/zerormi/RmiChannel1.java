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
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.LogStream;
import org.gridkit.zerormi.zlog.ZLogger;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RmiChannel1 implements RmiChannel {

    private static AtomicLong callId = new AtomicLong(0L);

    private final String name;
    private final OutputChannel messageOut;
    private final Executor callDispatcher;

    private final Map<Object, RemoteInstance> object2remote = new IdentityHashMap<Object, RemoteInstance>();
    private final Map<RemoteInstance, Object> remote2object = new HashMap<RemoteInstance, Object>();

    private final Map<RemoteInstance, Object> remoteInstanceProxys = new ConcurrentHashMap<RemoteInstance, Object>();
    private final Map<Long, RemoteCallContext> remoteReturnWaiters = new ConcurrentHashMap<Long, RemoteCallContext>();

    private final Map<RemoteMethodSignature, Method> methodCache = new ConcurrentHashMap<RemoteMethodSignature, Method>();
    private final RmiMarshaler marshaler;

    private final Map<String, Object> name2bean = new ConcurrentHashMap<String, Object>();
    private final Map<Object, String> bean2name = new ConcurrentHashMap<Object, String>();
    
    private final LogStream logCritical;
    
    private long debugRpcDelay = 0;

    private volatile boolean terminated = false;

    public RmiChannel1(String name, OutputChannel output, Executor callDispatcher, RmiMarshaler marshaler, ZLogger logger, Map<String, Object> props) {
        this.name = name;
        this.messageOut = output;
        this.callDispatcher = callDispatcher;
        this.marshaler = marshaler;
        this.logCritical = logger.get(getClass().getSimpleName(), LogLevel.CRITICAL);
        this.debugRpcDelay = readPropLong(props, "gridkit.zerormi.debug.rpc-delay", 0);
    }

    private long readPropLong(Map<String, Object> props, String key, long defaultValue) {
        if (props.get(key) != null) {
            Object v = props.get(key);
            return Long.valueOf(String.valueOf(v));
        }
        else {
            return Long.getLong(key, defaultValue);
        }
    }
    
    public void registerNamedBean(String name, Object obj) {
        name2bean.put(name, obj);
        bean2name.put(obj, name);
    }

    public void handleMessage(RemoteMessage message) {
        if (message instanceof RemoteCall) {

            final RemoteCall remoteCall = (RemoteCall) message;
            if (remoteCall.getArgs() != null) {
                for (int n = 0; n < remoteCall.getArgs().length; n++) {
                    Object arg = remoteCall.getArgs()[n];
                    if (arg instanceof RemoteInstance) {
                        RemoteInstance remoteInstance = (RemoteInstance) arg;
                        remoteCall.getArgs()[n] = getProxyFromRemoteInstance(remoteInstance);
                    }
                }
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    Thread.currentThread().setName("RemoteCall: " + remoteCall.toString());

                    try {
                        RemoteReturn remoteReturn;
                        try {
                            remoteReturn = delegateCall(remoteCall);
                        } catch (Exception e) {
                            e.printStackTrace();
                            RmiChannel1.this.close();
                            return;
                        }
                        try {
                            sendMessage(remoteReturn);
                        } catch (IOException e) {
                            RmiChannel1.this.close();
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
            RemoteCallContext context = remoteReturnWaiters.get(id);
            if (context == null) {
                throw new RuntimeException("Orphaned remote return: " + remoteReturn);
            }
            context.dispatch(remoteReturn);
            remoteReturnWaiters.remove(id);
        } else {
            throw new RuntimeException("Unknown RemoteMessage type. " + message); //$NON-NLS-1$
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
        for (RemoteCallContext context : remoteReturnWaiters.values()) {
            if (context.result == null) {
                context.result = new RemoteReturn(true, new RemoteException("Connection closed"), 0);
                LockSupport.unpark(context.thread);
            }
        }

        remoteReturnWaiters.clear();
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
            return new RemoteReturn(true, new RemoteException(String.format("Method %s cannot be resolved. %s", methodId, e.toString())), callId);
        }

        Object methodReturn = null;
        try {
            methodReturn = implementationMethod.invoke(implementator, remoteCall.getArgs());
            remoteReturn = new RemoteReturn(false, methodReturn, callId);
        } catch (InvocationTargetException e) {
            System.err.println("Call[" + remoteCall + "] exception " + e.getCause().toString());
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
    
    protected RemoteCallFuture asyncInvoke(final RemoteInstance remoteInstance, final Method method, Object[] args) {
    	Long id = generateCallId();
    	RemoteCall remoteCall = new RemoteCall(remoteInstance, new RemoteMethodSignature(method), args, id);
    	RemoteCallFuture future = new RemoteCallFuture(remoteCall);
    	
    	registerCall(future);

        try {
            sendMessage(remoteCall);
        }
        catch (IOException e) {
            remoteReturnWaiters.remove(future.remoteCall.callId);
            future.setErrorIfWaiting(e);
        }
    	
    	return future;
    }
    
    private void registerCall(RemoteCallFuture future) {
		RemoteCallContext ctx = new RemoteCallContext(future);

        if (terminated) {
            throw new IllegalStateException("Connection closed");
        }
        remoteReturnWaiters.put(future.remoteCall.callId, ctx);
	}

    @Override
	public Object remoteInvocation(final RemoteStub stub, final Object proxy, final Method method, final Object[] args) throws Throwable {

        Long id = generateCallId();
        RemoteInstance remoteInstance = stub.getRemoteInstance();
        RemoteMessage remoteCall = new RemoteCall(remoteInstance, new RemoteMethodSignature(method), args, id);

        RemoteCallContext context = new RemoteCallContext(Thread.currentThread());

        if (terminated) {
            throw new RemoteException("Connection closed");
        }
        // TODO race condition on close
        remoteReturnWaiters.put(id, context);
        try {
            sendMessage(remoteCall);
        }
        catch (IOException e) {
            throw decorateException(method, new RemoteException("Call failed", e));
        }

        if (debugRpcDelay > 0) {
            Thread.sleep(debugRpcDelay);
        }
        
        while (true) {
            if (terminated) {
            	throw decorateException(method, new RemoteException("Connection closed"));
            }
            long period = TimeUnit.SECONDS.toNanos(5);
            LockSupport.parkNanos(period);
            if (context.result != null) {
                break;
            } else if (terminated) {
                throw decorateException(method, new RemoteException("Connection closed"));
            } else if (Thread.interrupted()) {
                // TODO handle interruption
                throw decorateException(method, new InterruptedException());
            }
        }

        RemoteReturn ret = context.result;

        if (ret.throwing) {
            throw decorateException(method, modifyStackTrace(method, (Throwable) ret.getRet()));
        }

        return ret.getRet();
    }

    private Throwable modifyStackTrace(Method m, Throwable receiver) {
        if (receiver instanceof UndeclaredRemoteException) {
            receiver = receiver.getCause();
        }
//        StackTraceElement pboundary = new StackTraceElement("[" + name + "]", "<remote-call>", "", -1);
        StackTraceElement mframe = new StackTraceElement("[" + name + "] " + m.getDeclaringClass().getName(), m.getName(), "Remote call", -1);
        Exception donnor = new Exception();
        
        StackTraceElement[] rtrace = receiver.getStackTrace();
        StackTraceElement[] dtrace = donnor.getStackTrace();
        
        StackTraceElement[] result = new StackTraceElement[rtrace.length + dtrace.length + 2];
        
        
        int dr = findHighestStackMatch(dtrace);
        int rr = findLowestStackMatch(rtrace);

        int n = 0;
        
        for(int i = 0; i < rr; ++i) {
            result[n++] = rtrace[i];
        }
        
//        result[n++] = pboundary;
        result[n++] = mframe;

        for(int i = 0; i != dtrace.length; ++i) {
            if (i > dr) {
                result[n++] = dtrace[i];
            }
        }

        result = Arrays.copyOf(result, n);
        
        try {
            receiver.setStackTrace(result);
        } catch (Exception e) {
            // ignore
        }
        
        return receiver;
    }
    
    private int findLowestStackMatch(StackTraceElement[] trace) {
        boolean matched = false;
        for(int i = trace.length; i != 0;) {
            --i;
            if (matchStackFrame(trace[i])) {
                matched = true;
            }
            else if (matched) {
                return i + 1;
            }
            
        }
        return matched ? 0 : -1;
    }

    private int findHighestStackMatch(StackTraceElement[] trace) {
        boolean matched = false;
        for(int i = 0; i != trace.length; ++i) {
            if (matchStackFrame(trace[i])) {
                matched = true;
            }
            else if (matched) {
                return i - 1;
            }
        }
        return matched ? trace.length - 1 : trace.length;
    }
    
    
    private boolean matchStackFrame(StackTraceElement stackTraceElement) {
        if (stackTraceElement.getClassName().startsWith("org.gridkit.zerormi.RmiChannel")) {
            return true;
        }
        if (stackTraceElement.getClassName().startsWith("org.gridkit.zerormi.RmiGateway")) {
            return true;
        }
        if (stackTraceElement.getClassName().startsWith("org.gridkit.zerormi.RemoteStub")) {
            return true;
        }
        if (stackTraceElement.getClassName().startsWith("sun.reflect.")) {
            return true;
        }
        if (stackTraceElement.getClassName().startsWith("com.sun.proxy.")) {
            return true;
        }
        if (stackTraceElement.getClassName().startsWith("java.lang.reflect.Method")) {
            return true;
        }
        return false;
    }

    private Throwable decorateException(Method m, Throwable e) {
        boolean wrap = true;
        if (e instanceof RuntimeException || e instanceof Error) {
            wrap = false;
        }
        else {
            for(Class<?> c: m.getExceptionTypes()) {
                if (c.isInstance(e)) {
                    wrap = false;
                    break;
                }
            }
        }
        if (wrap) {
            return new UndeclaredRemoteException(e);
        }
        else {
            return e;
        }
    }
    
    @Override
    public FutureEx<Object> asyncRemoteInvocation(RemoteStub remoteStub, Object proxy, Method method, Object[] args) {
        return asyncInvoke(remoteStub.getRemoteInstance(), method, args);
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
            object2remote.put(proxy, remoteInstance);
        }
        return proxy;
    }

    public <T> void exportObject(Class<T> iface, T implementation) {
        exportObject(new Class[]{iface}, implementation);
    }

    @SuppressWarnings({ "rawtypes" })
    private synchronized RemoteInstance exportObject(Class[] interfaces, Object obj) {
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

    public synchronized Object streamResolveObject(Object obj) throws IOException {
    	
    	if (obj == null) {
    		return null;
    	}
    	
    	if (obj instanceof BeanRef) {
            BeanRef ref = (BeanRef) obj;
            Object bean = name2bean.get(ref.getBeanName());
            if (bean == null) {
                logCritical.log("Cannot resolve bean named '" + ref + "'");
            }
            return bean;
        }
        if (obj instanceof RemoteRef) {
        	RemoteRef ref = (RemoteRef) obj;
        	if (remote2object.containsKey(ref.getIdentity())) {
        		return remote2object.get(ref.getIdentity());
        	}
        	else {
        		return getProxyFromRemoteInstance(((RemoteRef) obj).getIdentity());
        	}
        } else {
            return marshaler.readResolve(obj);
        }
    }

    public synchronized Object streamReplaceObject(Object obj) throws IOException {
    	
    	if (obj == null) {
    		return null;
    	}
    	
        if (bean2name.containsKey(obj)) {
            return new BeanRef(bean2name.get(obj));
        }

        // allow explicit export
        RemoteInstance id = object2remote.get(obj);
        if (id != null) {
            return new RemoteRef(id);
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
        	Class<?> c = ReflectionHelper.primitiveToClass(names[i]);
            classes[i] = c != null ? c : classForName(names[i]);
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
        public final RemoteCallFuture future;
        public volatile RemoteReturn result;

        public RemoteCallContext(Thread thread) {
            this.thread = thread;
            this.future = null;
        }

        public RemoteCallContext(RemoteCallFuture future) {
            this.thread = null;
            this.future = future;
        }
        
        public void dispatch(RemoteReturn ret) {
            if (thread != null) {
                result = ret;
                LockSupport.unpark(thread);
            }
            else {
                if (ret.isThrowing()) {
                    future.setError((Throwable) ret.ret);
                }
                else {
                    future.setData(ret.ret);
                }
            }
        }
    }
    
    private static class RemoteCallFuture extends FutureBox<Object> {

		RemoteCall remoteCall;

    	public RemoteCallFuture(RemoteCall remoteCall) {
    		this.remoteCall = remoteCall;
		}
    }
}
