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
package org.gridkit.vicluster.isolate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.Thread.State;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.LogManager;

import org.gridkit.nanocloud.instrumentation.ByteCodeTransformer;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.VoidCallable.VoidCallableWrapper;

/**
 *	@author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@SuppressWarnings({ "deprecation" })
public class Isolate {

    private static final InheritableThreadLocal<Isolate> ISOLATE = new InheritableThreadLocal<Isolate>();

    private static class Multiplexer {

        private static PrintStream rootOut;
        private static PrintStream rootErr;
        private static Properties rootProperties;

        static {

            // initializing LogManager outside of isolate
            LogManager.getLogManager();

            boolean multiplex = !"true".equalsIgnoreCase(System.getProperty("gridkit.isolate.suppress.multiplexor", "false"));

            rootOut = System.out;
            rootErr = System.err;
            rootProperties = System.getProperties();

            if (multiplex) {

                System.err.println("Installing java.lang.System multiplexor");


                PrintStream mOut = new PrintStreamMultiplexor() {
                    @Override
                    protected PrintStream resolve() {
                        Isolate i = ISOLATE.get();
                        if (i == null) {
                            return rootOut;
                        }
                        else {
                            return INSIDE.get() == Boolean.TRUE ? rootOut : i.stdOut;
                        }
                    }
                };

                PrintStream mErr = new PrintStreamMultiplexor() {
                    @Override
                    protected PrintStream resolve() {
                        Isolate i = ISOLATE.get();
                        if (i == null) {
                            return rootErr;
                        }
                        else {
                            return INSIDE.get() == Boolean.TRUE ? rootErr : i.stdErr;
                        }
                    }
                };

                @SuppressWarnings("serial")
                Properties mProps = new PropertiesMultiplexor() {
                    @Override
                    protected Properties resolve() {
                        Isolate i = ISOLATE.get();
                        if (i == null) {
                            return rootProperties;
                        }
                        else {
                            return i.sysProps;
                        }
                    }
                };

                System.setOut(mOut);
                System.setErr(mErr);
                System.setProperties(mProps);
            }
        }
    }

    public static PrintStream getRootStdOut() {
        return Multiplexer.rootOut;
    }

    public static PrintStream getRootStdErr() {
        return Multiplexer.rootErr;
    }

    public static Isolate currentIsolate() {
        return ISOLATE.get();
    }

    public static Isolate getIsolate(ClassLoader cl) {
        if (cl instanceof IsolatedClassloader) {
            return ((IsolatedClassloader)cl).getIsolate();
        }
        else if (cl.getParent() != null) {
            return getIsolate(cl.getParent());
        }
        else {
            return null;
        }
    }

    private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = new HashMap<Class<?>, Object>();
    static {
        PRIMITIVE_DEFAULTS.put(boolean.class, Boolean.FALSE);
        PRIMITIVE_DEFAULTS.put(byte.class, Byte.valueOf((byte)0));
        PRIMITIVE_DEFAULTS.put(short.class, Short.valueOf((byte)0));
        PRIMITIVE_DEFAULTS.put(char.class, Character.valueOf((char)0));
        PRIMITIVE_DEFAULTS.put(int.class, Integer.valueOf((char)0));
        PRIMITIVE_DEFAULTS.put(long.class, Long.valueOf((char)0));
        PRIMITIVE_DEFAULTS.put(float.class, Float.valueOf(0f));
        PRIMITIVE_DEFAULTS.put(double.class, Double.valueOf(0f));
    }

    private static long MIN_THREAD_SCAN_INTERVAL = TimeUnit.MILLISECONDS.toNanos(100);

    private String name;
    private ThreadGroup threadGroup;
    private Thread isolateControlThread;
    private IsolatedClassloader cl;
    private ByteCodeTransformer classTransformer;
    private long lastThreadScan = System.nanoTime();

    private PrintStream stdOut;
    private PrintStream stdErr;
    private WrapperPrintStream wrpOut;
    private WrapperPrintStream wrpErr;
    private Properties sysProps;
    private int shutdownRetry = 0;

    private Map<String, Object> globals = new HashMap<String, Object>();
    private List<ThreadKiller> threadKillers = new ArrayList<ThreadKiller>();
    // TODO settle what thread-pool we need
    private ThreadPoolExecutor threadPool;

    private volatile BlockingQueue<WorkUnit> queue;

    public Isolate(String name, String... packages) {
        this.name = name;
        this.cl = new IsolatedClassloader(getClass().getClassLoader());
        if (packages.length > 0) {
            // legacy configuration style
            this.cl.addPackageRule("", false);
            for(String p: packages) {
                this.cl.addPackageRule(p, true);
            }
        }
        else {
            // new default, isolate all except JDK
            this.cl.addPackageRule("", true);
            this.cl.addRule(new ShareJreClasses());
        }

        threadGroup = new IsolateThreadGroup(name);

        sysProps = new Properties();
        sysProps.putAll(System.getProperties());
        sysProps.put("isolate.name", name);

        wrpOut = new WrapperPrintStream("[" + name + "] ", Multiplexer.rootOut);
        stdOut = new PrintStream(wrpOut);
        wrpErr = new WrapperPrintStream("[" + name + "] ", Multiplexer.rootErr);
        stdErr = new PrintStream(wrpErr);

        // TODO - remove once proper marshaling is implemented
        exclude(VoidCallable.class);
    }

    /**
     * This constructor is added to provide consistent classpath rule semantic across ViNodes.
     *
     * URLs from parent classloaders are ignored as resources.
     * Resources are searched in
     * - provided list of URLs
     * - baseResourceClassLoader
     *
     * @param name
     * @param resourceBaseClassloader
     * @param classpath
     */
    public Isolate(String name, ClassLoader resourceBaseClassloader, List<URL> classpath) {
        this(name);
        if (resourceBaseClassloader == null) {
            throw new NullPointerException("resourceBaseClassloader is null");
        }
        cl.resourceBaseClassloader = resourceBaseClassloader;
        for(URL u: classpath) {
            cl.addToClasspath(u);
        }
    }

    public String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
        // reinit name stuff
        // WARN threadGroup name will not be changed
        this.wrpOut.setPrefix("[" + name + "] ");
        this.wrpErr.setPrefix("[" + name + "] ");
        if (isolateControlThread != null) {
            isolateControlThread.setName("ISOLATE[" + name + "]");
        }
        updateThreadNames();
    }

    public PrintStream getStdOur() {
        return stdOut;
    }

    public PrintStream getStdErr() {
        return stdErr;
    }

    /**
     * Replace default wrapper print stream for stdOut.
     */
    public void replaceSdtOut(PrintStream stdOut) {
        this.stdOut = stdOut;
    }

    /**
     * Replace default wrapper print stream for stdErr.
     */
    public void replaceSdtErr(PrintStream stdErr) {
        this.stdErr = stdErr;
    }

    public Properties getSysProps() {
        return this.sysProps;
    }

    public void replaceSysProps(Properties props) {
        this.sysProps = props;
    }

    /**
     * Silence default print streams. Has no effect if print streams were replaced.
     */
    public void disableOutput() {
        wrpOut.setSilenced(true);
        wrpErr.setSilenced(true);
    }

    public void addThreadKiller(ThreadKiller killer) {
        threadKillers.add(killer);
    }

    public Object getGlobal(Class<?> domain, String name) {
        String propName = domain.getName() + ":" + name;
        return globals.get(propName);
    }

    public void setGlobal(Class<?> domain, String name, Object value) {
        String propName = domain.getName() + ":" + name;
        globals.put(propName, value);
    }

    public synchronized void start() {
        isolateControlThread = new Thread(threadGroup, new Runner());
        isolateControlThread.setName("ISOLATE[" + name + "]");
        isolateControlThread.setDaemon(true);
        queue = new SynchronousQueue<Isolate.WorkUnit>();
        isolateControlThread.start();
    }

    public void addPackageRule(List<Object> ruleList, String pack, boolean isolate) {
        if (pack.length() > 0 && !pack.endsWith(".")) {
            pack = pack + ".";
        }
        ruleList.add(new PackageIsolationRule(pack, isolate));
    }

    public void addClassRule(String className, boolean isolate) {
        if (className.indexOf('$') >= 0) {
            throw new IllegalArgumentException("You should provide top level class name: " + className);
        }
        cl.addClassRule(className, isolate);
    }

    public void addClassRule(List<Object> ruleList, String className, boolean isolate) {
        if (className.indexOf('$') >= 0) {
            throw new IllegalArgumentException("You should provide top level class name");
        }
        ruleList.add(new ClassIsolationRule(className, isolate));
    }

    public void addUrlRule(List<Object> ruleList, URL path, boolean isolate) {
        ruleList.add(new UrlIsolationRule(path, isolate));
    }

    public void addShareBootstrapRule(List<Object> ruleList) {
        ruleList.add(new ShareBootstrapClasses());
    }

    public void addShareJreRule(List<Object> ruleList) {
        ruleList.add(new ShareJreClasses());
    }

    public synchronized void applyRules(List<?> rules) {
        for(Object r: rules) {
            if (!(r instanceof IsolationRule)) {
                throw new IllegalArgumentException("Unsupported rule: " + r);
            }
        }
        for(Object r: rules) {
            cl.addRule((IsolationRule) r);
        }
    }

    public synchronized void addPackage(String packageName) {
        cl.addPackageRule(packageName, true);
    }

    /**
     * Classes marked as "excluded" will always be loaded from parent class loader.
     */
    public synchronized void exclude(String exclude) {
        cl.addClassRule(exclude, false);
    }

    /**
     * Classes marked as "excluded" will always be loaded from parent class loader.
     */
    public synchronized void exclude(Class<?>... excludes) {
        for(Class<?> c: excludes) {
            while(c.getDeclaringClass() != null) {
                c = c.getDeclaringClass();
            }
            cl.addClassRule(c.getName(), false);
        }
    }

    /**
     * Add URL to isolate class path.
     */
    public synchronized void addToClasspath(URL path) {
        cl.addToClasspath(path);
    }

    /**
     * Prohibit loading classes or resources from given URL via parent class loader.
     */
    public synchronized void removeFromClasspath(URL basePath) {
        cl.prohibitFromClasspath(basePath);
    }

    public synchronized void setByteCodeTransformer(ByteCodeTransformer bct) {
        if (classTransformer != null) {
            throw new IllegalArgumentException("ByteCodeTransformer is already set");
        }
        classTransformer = bct;
    }

    public String getProp(String prop) {
        return sysProps.getProperty(prop);
    }

    public void setProp(String prop, String value) {
        if (value == null) {
            sysProps.remove(prop);
        }
        else {
            sysProps.setProperty(prop, value);
        }
    }

    public void setProp(Map<String, String> props) {
        for(Map.Entry<String, String> e : props.entrySet()) {
            setProp(e.getKey(), e.getValue());
        }
    }

    private void abortablePut(WorkUnit wu) throws InterruptedException {
        while(true) {
            try {
                if (queue.offer(wu, 100, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // most likely JUnit is trying to kill us
                stdErr.println("Stoping isolate due to stop signal: " + e.toString());
                stop();
                AnyThrow.throwUncheked(e);
            }
        }
    }

    private Object process(WorkUnit wu) {
        try {
            abortablePut(wu);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Isolate[" + name + "] is not started");
        } catch (InterruptedException e) {
            AnyThrow.throwUncheked(e);
        }
        try {
            abortablePut(NOP);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Isolate[" + name + "] has been stopped");
        } catch (InterruptedException e) {
            e = convertOut(e);
            AnyThrow.throwUncheked(e);
        }

        Object result;
        if (wu instanceof CallableWorkUnit) {
            try {
                result = ((CallableWorkUnit<?>)wu).future.get();
            }
            catch(ExecutionException e) {
                e = convertOut(e);
                weaveAndRethrow(e.getCause());
                return null;
            }
            catch(Throwable e) {
                e = convertOut(e);
                AnyThrow.throwUncheked(e);
                return null;
            }
            return result;
        }
        else {
            return null;
        }
    }

    private void weaveAndRethrow(Throwable e) {
        Exception d = new Exception();

        StackTraceElement marker = new StackTraceElement(Isolate.class.getName(), "", null, -1);
        StackTraceElement boundary = new StackTraceElement("<isolate-boundary>", "<exec>", name, -1);

        ExceptionWeaver.replaceStackTop(e, marker, d, marker, boundary);
        AnyThrow.throwUncheked(e);
    }

    private void proxyWeaveAndRethrow(Object proxy, Throwable e) {
        Exception d = new Exception();

        StackTraceElement marker1 = new StackTraceElement("", "", null, -2); // match native method
        StackTraceElement marker2 = new StackTraceElement(proxy.getClass().getName(), "", null, -1);
        StackTraceElement boundary = new StackTraceElement("<isolate-boundary>", "<proxy>", name, -1);

        ExceptionWeaver.replaceStackTop(e, marker1, d, marker2, boundary);
        AnyThrow.throwUncheked(e);
    }

    /**
     * Executes runnable directly in isolated scope, bypassing marshaling.
     */
    @SuppressWarnings("rawtypes")
    public void execNoMarshal(Runnable task) {
        CallableWorkUnit wu = new CallableWorkUnit(task);
        process(wu);
    }

    @SuppressWarnings("rawtypes")
    public void exec(Runnable task) {
        CallableWorkUnit wu = new CallableWorkUnit((Runnable) convertIn(task));
        process(wu);
    }

    @SuppressWarnings("unchecked")
    public <V> V exec(Callable<V> task) {
        CallableWorkUnit<V> wu = new CallableWorkUnit<V>((Callable<V>) convertIn(task));
        return (V) convertOut(process(wu));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Future<Void> submit(Runnable task) {
        SubmitedWorkUnit wu = new SubmitedWorkUnit((Runnable) convertIn(task));
        process(wu);
        return translateFuture(wu.future);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Future<Void> submitNoMarshal(Runnable task) {
        SubmitedWorkUnit wu = new SubmitedWorkUnit((Runnable) task);
        process(wu);
        return wu.future;
    }

    @SuppressWarnings("unchecked")
    public <V> Future<V> submit(Callable<? extends V> task) {
        SubmitedWorkUnit<V> wu = new SubmitedWorkUnit<V>((Callable<V>) convertIn(task));
        process(wu);
        return translateFuture(wu.future);
    }


    @SuppressWarnings("unchecked")
    public <V> V export(Callable<V> task) {
        CallableWorkUnit<V> wu = new CallableWorkUnit<V>((Callable<V>) convertIn(task));
        return (V) exportOut(process(wu));
    }

    @SuppressWarnings("unchecked")
    public <V> V exportNoProxy(Callable<V> task) {
        CallableWorkUnit<V> wu = new CallableWorkUnit<V>((Callable<V>) convertIn(task));
        return (V) process(wu);
    }

    protected <V> Future<V> translateFuture(Future<V> future) {
        return new IsolateFuture<V>(future);
    }

    /**
     * Translates object in in-Isolate form.
     *
     * @deprecated for advanced use
     */
    @SuppressWarnings("unchecked")
    public <T> Object convertIn(T obj) {
        if (obj != null && obj.getClass() == VoidCallableWrapper.class) {
            // special hack for void callable
            return new VoidCallableWrapper((VoidCallable)convertIn(((VoidCallableWrapper)obj).callable));
        }
        if (obj != null && !(obj instanceof Serializable) && obj.getClass().isAnonymousClass()) {
            try {
                return (T)convertAnonimous(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return fromBytes(toBytes(obj), cl);
    }

    @SuppressWarnings("unchecked")
    protected <V> V convertOut(Object obj) {
        return (V) fromBytes(toBytes(obj), cl.getParent());
    }

    protected Object exportOut(Object obj) {
        Class<?>[] interfaces = convertOut(collectInterfaces(obj.getClass()));
        ProxyOut po = new ProxyOut(obj, interfaces);
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, po);
    }

    @SuppressWarnings("rawtypes")
    protected Class[] collectInterfaces(Class<?> type) {
        Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        collectInterfaces(type, interfaces);
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    private void collectInterfaces(Class<?> type, Set<Class<?>> collector) {
        if (type.isInterface()) {
            collector.add(type);
        }
        for(Class<?> i : type.getInterfaces()) {
            collectInterfaces(i,collector);
        }
    }

    protected boolean isIsolatedClass(Class<?> x) {
        ClassLoader cl = x.getClassLoader();
        while(cl != null) {
            if (cl == this.cl) {
                return true;
            }
            cl = cl.getParent();
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    protected Object convertAnonimous(Object obj) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Class c_out = obj.getClass();
        Class c_in = cl.loadClass(c_out.getName());

        if (c_in == c_out) {
            return obj;
        }
        else {
            Field[] f_out = collectFields(c_out);
            Field[] f_in = collectFields(c_in);
            Constructor<?> c = c_in.getDeclaredConstructors()[0];

            c.setAccessible(true);
            // we have to init primitive params, cause null cannot be converted to primitive value
            Object[] params = new Object[c.getParameterTypes().length];
            for(int i = 0; i != params.length; ++i) {
                Class<?> p = c.getParameterTypes()[i];
                params[i] = PRIMITIVE_DEFAULTS.get(p);
            }
            Object oo = c.newInstance(params);

            for(Field fo : f_out) {
                if (fo.getName().startsWith("this$")) {
                    continue;
                }
                fo.setAccessible(true);
                Object v = fo.get(obj);
                for(Field fi : f_in) {
                    if (fi.getName().equals(fo.getName()) && fi.getDeclaringClass().getName().equals(fo.getDeclaringClass().getName())) {
                        fi.setAccessible(true);
                        fi.set(oo, convertIn(v));
                    }
                }
            }

            return oo;
        }
    }

    private Field[] collectFields(Class<?> c) {
        List<Field> result = new ArrayList<Field>();
        collectFields(result, c);
        return result.toArray(new Field[result.size()]);
    }

    private void collectFields(List<Field> result, Class<?> c) {
        Class<?> s = c.getSuperclass();
        if (s != Object.class) {
            collectFields(result, s);
        }
        for(Field f: c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                result.add(f);
            }
        }
    }

    private void updateThreadNames() {
        if (threadGroup == null || lastThreadScan + MIN_THREAD_SCAN_INTERVAL > System.nanoTime()) {
            return;
        }
        updateThreadNames(threadGroup);
    }

    private void updateThreadNames(ThreadGroup tg) {
        String prefix = "ISOLATE[" + name +"] ";
        Thread[] threads = new Thread[2 * tg.activeCount()];
        int n = tg.enumerate(threads);
        if (n > threads.length) {
            n = threads.length;
        }
        for(int i = 0; i != n; ++i) {
            try {
                Thread thread = threads[i];
                if (!thread.getName().startsWith(prefix)) {
                    String nn = thread.getName();
                    if (nn.startsWith("ISOLATE")) {
                        nn = nn.substring(nn.indexOf(']') + 1);
                    }
                    nn = prefix + nn;
                    thread.setName(nn);
                }
            }
            catch(Exception e) {
                // ignore
            }
        }

        ThreadGroup[] subgroups = new ThreadGroup[tg.activeGroupCount()];
        n = tg.enumerate(subgroups);
        if (n > subgroups.length) {
            n = subgroups.length;
        }

        for(int i = 0; i != n; ++i) {
            updateThreadNames(subgroups[i]);
        }
    }

//	@SuppressWarnings("deprecation")
    public void suspend() {
        threadGroup.suspend();
    }

//	@SuppressWarnings("deprecation")
    public void resume() {
        threadGroup.resume();
    }

    public void stop() {
        try {
            queue.offer(STOP, 1000, TimeUnit.SECONDS);
            while(queue != null) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // ignore;
        }
        stdErr.println("Stopping ...");
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                // give thread pool a chance to gracefully shutdown itself
                threadPool.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        while(true) {
            while( 0 < kill(threadGroup)
                    + removeShutdownHooks()
                    + removeAppContexts() ) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
                ++shutdownRetry;
                if (shutdownRetry >  1000) {
                    break;
                }
            }
            try {
                if (!threadGroup.isDestroyed()) {
                    threadGroup.destroy();
                }
                break;
            }
            catch(IllegalThreadStateException e) {
                stdErr.println(e);
            }
            if (shutdownRetry >  1000) {
                stdErr.println("Isolate clean up failed");
                break;
            }
        }
        threadKillers = null;
        cl = null;
        threadGroup = null;
        isolateControlThread = null;
        sysProps = null;

        stdErr.println("Stopped");

        System.gc();
        Runtime.getRuntime().runFinalization();
    }

    private int kill(ThreadGroup tg) {
        int threadCount = 0;

        Thread[] threads = new Thread[2 * tg.activeCount()];
        int n = tg.enumerate(threads);
        for(int i = 0; i != n; ++i) {
            ++threadCount;
            Thread t = threads[i];
            try {
                if (Runtime.getRuntime().removeShutdownHook(t)) {
                    stdErr.println("Removing shutdown hook: " + t.getName());
                }
            }
            catch(IllegalStateException e) {
                // ignore
            }
            if (t.getState() != State.TERMINATED) {
                killThread(t, false);
            }
            else {
                if (shutdownRetry % 10 == 9) {
                    stdErr.println("Already terminated: " + t.getName());
                }
            }

            if (t.isAlive() && shutdownRetry > 24) {
                stdErr.println("Killing: " + t.getName());
                if (shutdownRetry > 30 && (shutdownRetry % 10 == 5)) {
                    StackTraceElement[] trace = t.getStackTrace();
                    for(StackTraceElement e: trace) {
                        stdErr.println("  at " + e);
                    }
                }
                try {
                    killThread(t, true);
                }
                catch(Exception e) {
                    stdErr.println("Socket interruption failed: " + e.toString());
                }
            }
        }

        ThreadGroup[] groups = new ThreadGroup[2 * tg.activeGroupCount()];
        n = tg.enumerate(groups);
        for(ThreadGroup g: groups) {
            if (g != null) {
                threadCount += kill(g);
            }
        }

        return threadCount;
    }

    private void killThread(Thread t, boolean force) {
        if (!force) {
            try { t.resume(); }	catch(Exception e) {/* ignore */};
            try { t.start(); }	catch(Exception e) {/* ignore */};
            try { t.interrupt(); }	catch(Exception e) {/* ignore */};
            ThreadHelper.stopThread(name, t);
        }
        else {
            try { t.interrupt(); }	catch(Exception e) {/* ignore */};
            trySocketInterrupt(t);
            tryStop(t);
            try { t.interrupt(); }	catch(Exception e) {/* ignore */};
            ThreadHelper.stopThread(name, t);
        }
    }

    private void trySocketInterrupt(Thread t) {
        Object target = getField(t, "target");
        if (target == null) {
            return;
        }
        String cn = target.getClass().getName();
        if (cn.startsWith("com.tangosol.coherence.component")
                && cn.contains("PacketListener")) {
            try {
                Object udpSocket = getField(target, "__m_UdpSocket");
                DatagramSocket ds = (DatagramSocket) getField(udpSocket, "__m_DatagramSocket");
                ds.close();
                stdErr.println("Closing socket for " + t.getName());
            }
            catch(Exception e) {
                // ignore
            }
        }
        else if (cn.startsWith("com.tangosol.coherence.component")
                    && cn.contains("PacketPublisher")) {
            try {
                Object udpSocket = getField(target, "__m_UdpSocketUnicast");
                DatagramSocket ds = (DatagramSocket) getField(udpSocket, "__m_DatagramSocket");
                ds.close();
                stdErr.println("Closing socket for " + t.getName());
            }
            catch(Exception e) {
                // ignore;
            }
            try {
                Object udpSocket = getField(target, "__m_UdpSocketMulticast");
                DatagramSocket ds = (DatagramSocket) getField(udpSocket, "__m_DatagramSocket");
                ds.close();
                stdErr.println("Closing socket for " + t.getName());
            }
            catch(Exception e) {
                // ignore;
            }
        }
    }

    // TODO plugable thread killers
    private void tryStop(Thread thread) {
        Object target = getField(thread, "target");
        if (target != null) {
            try {
                Method m = target.getClass().getMethod("stop");
                m.setAccessible(true);
                m.invoke(target);
                stdErr.println("Calling stop on " + thread.getName());
            } catch (Exception e) {
                //ignore
            }
        }
    }

    private static Object getField(Object x, String field) {
        try {
            Field f = null;
            Class<?> c = x.getClass();
            while(f == null && c != Object.class) {
                try {
                    f = c.getDeclaredField(field);
                } catch (NoSuchFieldException e) {
                }
                if (f == null) {
                    c = c.getSuperclass();
                }
            }
            if (f != null) {
                f.setAccessible(true);
                return f.get(x);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("Cannot get '" + field + "' from " + x.getClass().getName());
    }

    boolean isOwnedThreadGroup(ThreadGroup tg) {
        while(true) {
            if (tg == threadGroup) {
                return true;
            }
            else if (tg.getParent() == null || tg.getParent() == tg) {
                return false;
            }
            else {
                tg = tg.getParent();
            }
        }
    }

    private int removeShutdownHooks() {
        int threadCount = 0;
        for(Thread t : getSystemShutdownHooks()) {
            if (isOwnedThreadGroup(t.getThreadGroup()) || t.getContextClassLoader() == cl) {
                ++threadCount;
                try {
                    if (Runtime.getRuntime().removeShutdownHook(t)) {
                        stdErr.println("Removing shutdown hook: " + t.getName());
                    }
                }
                catch(IllegalStateException e) {
                    // ignore
                }
            }
        }
        return threadCount;
    }

    @SuppressWarnings("unchecked")
    private Collection<Thread> getSystemShutdownHooks() {
        try {
            Class<?> cls = Class.forName("java.lang.ApplicationShutdownHooks");
            Field f = cls.getDeclaredField("hooks");
            f.setAccessible(true);
            Map<Thread, Thread> hooks = (Map<Thread, Thread>) f.get(null);
            return new ArrayList<Thread>(hooks.values());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("restriction")
    private int removeAppContexts() {
        int n = 0;
        Set<sun.awt.AppContext> contexts = sun.awt.AppContext.getAppContexts();
        Iterator<sun.awt.AppContext> it = contexts.iterator();
        while(it.hasNext()) {
            sun.awt.AppContext ctx = it.next();
            if (isOwnedThreadGroup(ctx.getThreadGroup())) {
                ++n;
                it.remove();
                stdErr.println("Removing AppContext: " + ctx.toString());
            }
        }
        return n;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return cl.loadClass(name);
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    private class IsolateThreadGroup extends ThreadGroup {

        private IsolateThreadGroup(String name) {
            super(name);
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (e instanceof ThreadDeath) {
                // ignore
            }
            else {
                stdErr.println("Uncaught exception at thread " + t.getName());
                e.printStackTrace(stdErr);
            }
        }
    }

    private interface WorkUnit {
        public void exec() throws Exception;
    }

    private static StopMarker STOP = new StopMarker();

    private static class StopMarker implements WorkUnit {
        @Override
        public void exec() {
            throw new UnsupportedOperationException();
        }
    }

    private static Nop NOP = new Nop();

    private static class Nop implements WorkUnit {
        @Override
        public void exec() {
            // nop
        }
    }

    private static byte[] toBytes(Object x) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(x);
            oos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private static Object fromBytes(byte[] serialized) {
        return fromBytes(serialized, Thread.currentThread().getContextClassLoader());
    }

    private static Object fromBytes(byte[] serialized, final ClassLoader cl) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized)) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    String name = desc.getName();
                    try {
                        return Class.forName(name, false, cl);
                    } catch (ClassNotFoundException ex) {
                        return super.resolveClass(desc);
                    }
                }
            };
            return ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class ProxyOut implements InvocationHandler {

        private Map<Method, Method> methodMap = new HashMap<Method, Method>();
        private Object target;

        public ProxyOut(Object target, Class<?>[] interfaces) {
            this.target = target;
            for(Class<?> i : interfaces) {
                try {
                    mapMethods(i);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void mapMethods(Class<?> i) throws SecurityException, NoSuchMethodException, ClassNotFoundException {
            if (i.getDeclaredMethods() != null) {
                for(Method m : i.getDeclaredMethods()) {
                    if (!Modifier.isStatic(m.getModifiers())) {
                        Method m2 = target.getClass().getMethod(m.getName(), convertClassesIn(m.getParameterTypes()));
                        m2.setAccessible(true);
                        methodMap.put(m, m2);
                    }
                }
            }
            if (i.getInterfaces() != null) {
                for(Class<?> ii: i.getInterfaces()) {
                    mapMethods(ii);
                }
            }
        }

        @SuppressWarnings("rawtypes")
        private Class<?>[] convertClassesIn(Class<?>[] cls) throws ClassNotFoundException {
            Class[] cls2 = new Class[cls.length];
            int n = 0;
            for(Class c: cls) {
                if (c.isPrimitive()) {
                    cls2[n++] = c;
                }
                else if (c.isArray()) {
                    Class cc = c.getComponentType();
                    if (c.getComponentType().isPrimitive()) {
                        cls2[n++] = c;
                    }
                    else {
                        Class cc2= convertClassesIn(new Class[]{cc})[0];
                        cls2[n++] = Array.newInstance(cc2, 0).getClass();
                    }
                }
                else {
                    cls2[n++] = cl.loadClass(c.getName());
                }
            }
            return cls2;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method m2 = methodMap.get(method);
            Object[] args2 = null;
            if (args != null) {
                args2 = new Object[args.length];
                for(int i = 0; i != args.length; ++i) {
                    args2[i] = convertIn(args[i]);
                }
            }

            try {
                Object r = m2.invoke(target, args2);
                if (r != null) {
                    return convertOut(r);
                }
                else {
                    return null;
                }
            }
            catch(InvocationTargetException e) {
                proxyWeaveAndRethrow(proxy, (Throwable)convertOut(e.getCause()));
                return null;
            }
        }
    }

    private static class CallableWorkUnit<V> implements WorkUnit {

        final FutureTask<V> future;

        public CallableWorkUnit(final Callable<V> x) {
            future = new FutureTask<V>(new Callable<V>() {
                // Callable wrapper to simplify stack trace weaving
                @Override
                public V call() throws Exception {
                    return x.call();
                }
            });
        }

        public CallableWorkUnit(final Runnable x) {
            future = new FutureTask<V>(new Runnable(){
                // Runnable wrapper to simplify stack trace weaving
                @Override
                public void run() {
                    x.run();
                }

            },(V) null);
        }

        @Override
        public void exec() throws Exception {
            future.run();
        }
    }

    private class SubmitedWorkUnit<V> implements WorkUnit {

        final Object task;
        Future<V> future;

        public SubmitedWorkUnit(final Callable<V> task) {
            this.task = task;
        }

        public SubmitedWorkUnit(final Runnable task) {
            this.task = task;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void exec() throws Exception {
            if (task instanceof Runnable) {
                Runnable tw = new IsolateTaskMarker<Void>(((Runnable)task));
                future = (Future<V>) threadPool.submit(tw);
            }
            else {
                Callable tw = new IsolateTaskMarker((Callable)task);
                future = (Future<V>) threadPool.submit(tw);
            }
        }
    }

    private class IsolateFuture<V> implements Future<V> {

        private Future<V> inner;

        public IsolateFuture(Future<V> inner) {
            this.inner = inner;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                return inner.cancel(mayInterruptIfRunning);
            }
            catch(Exception e) {
                AnyThrow.throwUncheked((Exception)convertOut(e));
                throw new Error("Unreachable code");
            }
        }

        @Override
        public boolean isCancelled() {
            try {
                return inner.isCancelled();
            }
            catch(Exception e) {
                AnyThrow.throwUncheked((Exception)convertOut(e));
                throw new Error("Unreachable code");
            }
        }

        @Override
        public boolean isDone() {
            try {
                return inner.isDone();
            }
            catch(Exception e) {
                AnyThrow.throwUncheked((Exception)convertOut(e));
                throw new Error("Unreachable code");
            }
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            try {
                return convertOut(inner.get());
            }
            catch(Exception e) {
                AnyThrow.throwUncheked((Exception)convertOut(e));
                throw new Error("Unreachable code");
            }
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return convertOut(inner.get());
            }
            catch(Exception e) {
                AnyThrow.throwUncheked((Exception)convertOut(e));
                throw new Error("Unreachable code");
            }
        }
    }

    private static class IsolateTaskMarker<V> implements Runnable, Callable<V> {

        private Runnable runnable;
        private Callable<V> callable;

        public IsolateTaskMarker(Runnable runnable) {
            this.runnable = runnable;
        }

        public IsolateTaskMarker(Callable<V> callable) {
            this.callable = callable;
        }

        @Override
        public V call() throws Exception {
            return callable.call();
        }

        @Override
        public void run() {
            runnable.run();
        }
    }


    private class Runner implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setContextClassLoader(cl);
            ISOLATE.set(Isolate.this);
            Thread.currentThread().setName("main");

            // should be initialized inside of Isolate
            threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

            try{
                while(true) {
                    updateThreadNames();
                    try {
                        WorkUnit unit = queue.poll(1000, TimeUnit.MILLISECONDS);
                        if (unit == null) {
                            continue;
                        }
                        if (unit instanceof StopMarker) {
                            break;
                        }
                        else {
                            unit.exec();
                        }
                    }
                    catch (ThreadDeath e) {
                        return;
                    }
                    catch (Exception e) {
                        System.err.println("Exception in isolate [" + name + "]");
                        e.printStackTrace();
                    };
                }
            }
            finally{
                queue = null;
            }
        };
    }



    private interface IsolationRule {

        public Boolean shouldIsolate(URL resource, String className);

    }

    private class IsolationRuleSet implements IsolationRule {

        private List<IsolationRule> rules = new ArrayList<Isolate.IsolationRule>();

        public void addRule(IsolationRule rule) {
            rules.add(0, rule);
        }

        @Override
        public Boolean shouldIsolate(URL resource, String className) {
            for(IsolationRule rule: rules) {
                Boolean b = rule.shouldIsolate(resource, className);
                if (b != null) {
                    return b;
                }
            }
            return null;
        }
    }

    private static List<URL> listBootstrapClasspath() {
        String bcp = System.getProperty("sun.boot.class.path");
        List<URL> result = new ArrayList<URL>();
        for(String path: bcp.split("\\" + System.getProperty("path.separator"))) {
            try {
                addEntriesFromManifest(result, new File(path).toURI().toURL());
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        return result;
    }

    private static void addEntriesFromManifest(List<URL> list, URL url) {
        try {
            InputStream is;
            is = url.openStream();
            if (is != null) {
                list.add(url);
                try {
                    JarInputStream jar = new JarInputStream(is);
                    list.add(new URL("jar:" + url.toString() + "!/"));
                    Manifest mf = jar.getManifest();
                    jar.close();
                    if (mf == null) {
                        return;
                    }
                    String cp = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
                    if (cp != null) {
                        for(String entry: cp.split("\\s+")) {
                            try {
                                URL ipath = new URL(url, entry);
                                addEntriesFromManifest(list, ipath);
                            }
                            catch(Exception e) {
                                // ignore
                            }
                        }
                    }
                }
                catch(IOException e) {
                    throw e;
                }
            }
        }
        catch(Exception e) {
        }
    }

    private static boolean belongs(URL base, URL derived) {
        // TODO not exactly correct, but should work
        return derived.toString().startsWith(base.toString());
    }

    private static boolean belongs(List<URL> baseList, URL derived) {
        for(URL base: baseList) {
            if (belongs(base, derived)) {
                return true;
            }
        }
        return false;
    }

    private static class ShareBootstrapClasses implements IsolationRule {

        private final List<URL> bootclassPath = listBootstrapClasspath();

        @Override
        public Boolean shouldIsolate(URL resource, String className) {
            if (belongs(bootclassPath, resource)) {
                return Boolean.FALSE;
            }
            else {
                return null;
            }
        }
    }

    private static class ShareJreClasses implements IsolationRule {

        private final URL jvmHome;

        public ShareJreClasses() {
            try {
                jvmHome = getJreRoot();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static URL getJavaHome() throws MalformedURLException {
            return new File(System.getProperty("java.home")).toURI().toURL();
        }

        private static URL getJreRoot() throws MalformedURLException {
            String jlo = ClassLoader.getSystemResource("java/lang/Object.class").toString();
            String root = jlo;
            if (root.startsWith("jar:")) {
                root = root.substring("jar:".length());
                int n = root.indexOf('!');
                root = root.substring(0, n);

                if (root.endsWith("/rt.jar")) {
                    root = root.substring(0, root.lastIndexOf('/'));
                    if (root.endsWith("/lib")) {
                        root = root.substring(0, root.lastIndexOf('/'));
                        return new URL(root);
                    }
                }
            }
            // fall back
            return getJavaHome();
        }

        @Override
        public Boolean shouldIsolate(URL resource, String className) {
            if ("jar".equals(resource.getProtocol())) {
                String path = resource.getPath();
                int n = path.indexOf("!");
                if (n > 0) {
                    path = path.substring(0, n);
                }
                try {
                    resource = new URL(path);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (belongs(jvmHome, resource)) {
                return Boolean.FALSE;
            }
            else {
                return null;
            }
        }

        @Override
        public String toString() {
            return "ShareJreClasses[" + jvmHome + "]";
        }
    }

    private static class PackageIsolationRule implements IsolationRule {

        private final String prefix;
        private final boolean isolate;

        public PackageIsolationRule(String prefix, boolean isolate) {
            this.prefix = prefix;
            this.isolate = isolate;
        }

        @Override
        public Boolean shouldIsolate(URL resource, String className) {
            return className.startsWith(prefix) ? isolate : null;
        }
    }

    private static class ClassIsolationRule implements IsolationRule {

        private final String className;
        private final boolean isolate;

        public ClassIsolationRule(String className, boolean isolate) {
            this.className = className;
            this.isolate = isolate;
        }

        @Override
        public Boolean shouldIsolate(URL resource, String className) {
            int n = className.indexOf("$");
            if (n >= 0) {
                String topClass = className.substring(0, n);
                return this.className.equals(topClass) ? isolate : null;
            }
            else {
                return this.className.equals(className) ? isolate : null;
            }
        }
    }

    private static class UrlIsolationRule implements IsolationRule {

        private final URL urlPath;
        private final boolean isolate;

        public UrlIsolationRule(URL urlPath, boolean isolate) {
            this.urlPath = urlPath;
            this.isolate = isolate;
        }

        @Override
        public Boolean shouldIsolate(URL resource, String className) {
            return belongs(urlPath, resource) ? isolate : null;
        }
    }

    private class IsolatedClassloader extends ClassLoader {

        protected ClassLoader baseClassloader;
        protected ClassLoader resourceBaseClassloader;

        protected IsolationRuleSet rules;

        protected List<URL> externalPaths = new ArrayList<URL>();
        protected URLClassLoader cpExtention;
        protected List<String> forbidenPaths = new ArrayList<String>();

        protected ProtectionDomain isolateDomain;
        protected Map<URL, ProtectionDomain> domainCache = new HashMap<URL, ProtectionDomain>();

        protected boolean shouldDecorateURLs() {
            return "true".equalsIgnoreCase(sysProps.getProperty("gridkit.isolate.class-source-decoration"));
        }

        protected boolean shouldTraceIsolation() {
            return "true".equalsIgnoreCase(sysProps.getProperty("gridkit.isolate.trace-classes"));
        }

        protected boolean shouldTraceInstrumentation() {
            return "true".equalsIgnoreCase(sysProps.getProperty("gridkit.isolate.trace-instrumentation"));
        }

        IsolatedClassloader(ClassLoader base) {
            super(null);
            this.baseClassloader = base;
            this.rules = new IsolationRuleSet();

            PermissionCollection pc = new Permissions();
            pc.add(new AllPermission());

            ProtectionDomain domain = new ProtectionDomain(
                    /* codesource - */ getClass().getProtectionDomain().getCodeSource(),
                    /* permissions - */ pc,
                    /* classloader - */ null,
                    /* principals - */ getClass().getProtectionDomain().getPrincipals());
            this.isolateDomain = domain;
        }

        public Isolate getIsolate() {
            return Isolate.this;
        }

        public void addRule(IsolationRule rule) {
            rules.addRule(rule);
        }

        public void addPackageRule(String prefix, boolean isolate) {
            if (prefix.length() > 0 && !prefix.endsWith(".")) {
                prefix = prefix + ".";
            }
            rules.addRule(new PackageIsolationRule(prefix, isolate));
        }

        public void addClassRule(String className, boolean isolate) {
            rules.addRule(new ClassIsolationRule(className, isolate));
        }

        /**
         * Prohibits loading classes or resources from specific URL is isolate.
         */
        public void prohibitFromClasspath(URL basePath) {
            forbidenPaths.add(basePath.toString());
        }

        /**
         * Add additional URL to isolate classpath.
         */
        public synchronized void addToClasspath(URL path) {
            externalPaths.add(path);
            cpExtention = null;
        }

        @Override
        public void clearAssertionStatus() {
            baseClassloader.clearAssertionStatus();
        }

        @Override
        public synchronized URL getResource(String name) {
            Enumeration<URL> all;
            try {
                all = getResources(name);
            } catch (IOException e) {
                return null;
            }
            if (all.hasMoreElements()) {
                return all.nextElement();
            }
            else {
                return null;
            }
        }

        @Override
        public synchronized Enumeration<URL> getResources(String name) throws IOException {
            if (cpExtention == null) {
                cpExtention = new URLClassLoader(externalPaths.toArray(new URL[0]));
            }
            Vector<URL> result = new Vector<URL>();
            URL r;
            Enumeration<URL> en = cpExtention.findResources(name);
            while(en.hasMoreElements()) {
                r = en.nextElement();
                if (!result.contains(r) && !isForbiden(r)) {
                    result.add(r);
                }
            }

            if (resourceBaseClassloader != null) {
                en = resourceBaseClassloader.getResources(name);
            }
            else {
                en = baseClassloader.getResources(name);
            }

            while(en.hasMoreElements()) {
                r = en.nextElement();
                if (!result.contains(r) && !isForbiden(r)) {
                    result.add(r);
                }
            }

            return result.elements();
        }

        private boolean isForbiden(URL r) {
            if (r != null && !forbidenPaths.isEmpty()) {
                String s = r.toString();
                for(String path: forbidenPaths) {
                    // TODO not exactly correct, but unlikely to cause troubles
                    if (s.startsWith(path)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void setClassAssertionStatus(String className, boolean enabled) {
            baseClassloader.setClassAssertionStatus(className, enabled);
        }

        @Override
        public void setDefaultAssertionStatus(boolean enabled) {
            baseClassloader.setDefaultAssertionStatus(enabled);
        }

        @Override
        public void setPackageAssertionStatus(String packageName, boolean enabled) {
            baseClassloader.setPackageAssertionStatus(packageName, enabled);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> cc = findLoadedClass(name);
            if (cc == null) {
                if (!isInterallyShared(name)) {
                    String bytepath = name.replace('.', '/') + ".class";
                    URL url = getResource(bytepath);
                    if (url == null) {
                        throw new ClassNotFoundException(name);
                    }
                    URL baseurl = baseClassloader.getResource(bytepath);
                    if (isInterallyIsolated(name)
                            || baseurl == null
                            || shouldIsolate(url, name)) {
                        Class<?> cl = findLoadedClass(name);
                        if (cl == null) {
                            cl = findClass(name);
                        }
                        if (cl == null) {
                            throw new ClassNotFoundException(name);
                        }
                        if (resolve) {
                            resolveClass(cl);
                        }
                        return cl;
                    }
                }
//				if (name.equals("sun.awt.AppContext")) {
//					new Exception("loading AppContext").printStackTrace();
//				}
                cc = baseClassloader.loadClass(name);
            }
            if (resolve) {
                resolveClass(cc);
            }
            return cc;
        }

        private boolean shouldIsolate(URL url, String name) throws ClassNotFoundException {
            Boolean isolate = rules.shouldIsolate(url, name);
            if (isolate == null) {
                throw new ClassNotFoundException("No isolation rule for [" + name + "]");
            }
            return isolate;
        }

        private boolean isInterallyShared(String name) {
            if (name.startsWith("java.") || name.startsWith("jdk.")) {
                return true;
            }
            else if (name.equals(Isolate.class.getName())
                    || name.startsWith(Isolate.class.getName() + "$")
                    || name.equals(ThreadKiller.class.getName())
                    || name.equals(VoidCallable.class.getName())
                    // intsrumentation related interfaces
                    || name.equals(ByteCodeTransformer.class.getName())
//					|| name.equals(Interceptor.class.getName())
//					|| name.equals(Interception.class.getName())
                    ) {
                return true;
            }
            else {
                return false;
            }
        }

        private boolean isInterallyIsolated(String name) {
            if (name.startsWith("org.gridkit.zerormi.")) {
                // TODO Until migration to new comm layer, we have to force ZeroRMI isolation.
                return true;
            }
            else {
                return false;
            }
        }

        @SuppressWarnings("unused")
        private byte[] asBytes(InputStream is) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4 << 10];
                while(true) {
                    int n = is.read(buf);
                    if (n < 0) {
                        return bos.toByteArray();
                    }
                    else if (n != 0) {
                        bos.write(buf, 0, n);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected Class<?> findClass(String classname) throws ClassNotFoundException {
            try {
                String path = classname.replace('.', '/').concat(".class");
                URL url = getResource(path);
                InputStream res = url.openStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                while(true) {
                    int x = res.read(buf);
                    if (x <= 0) {
                        res.close();
                        break;
                    }
                    else {
                        bos.write(buf, 0, x);
                    }
                }
                byte[] cd = bos.toByteArray();
                try {
                    return defineClass(url, classname, cd);
                }
                catch(OutOfMemoryError e) {
                    // try it again
                    System.gc();
                    System.runFinalization();
                    System.gc();
                    return defineClass(url, classname, cd);
                }
            }
            catch(Exception e) {
                throw new ClassNotFoundException(classname);
            }
        }

        private Class<?> defineClass(URL url, String classname, byte[] cd) throws ClassFormatError {
            if (shouldTraceIsolation()) {
                getRootStdOut().println("<" + getName() + "> isolate class - " + classname);
            }
            if (classTransformer != null) {
                // TODO use classhierarchy helper
                try {
                    byte[] newcd = classTransformer.rewriteClassData(classname, cd, null);
                    if (shouldTraceInstrumentation() && cd != newcd) {
                        getRootStdOut().println("<" + getName() + "> instrumented class - " + classname);
                    }
                    cd = newcd;
                }
                catch(Exception e) {
                    getRootStdErr().println("<" + getName() + "> class transformation failed - " + classname);
                    e.printStackTrace(getRootStdErr());
                }
            }
            Class<?> c = defineClass(classname, cd, 0, cd.length, getProtectionDomain(url));
            ensurePackage(classname, url);
            return c;
        }

        private void ensurePackage(String classname, URL url) {
            if (classname.indexOf('.') > 0) {
                String pname = classname.substring(0, classname.lastIndexOf('.'));
                if (getPackage(pname) == null) {
                    URL baseUrl = getBaseURL(url, classname);
                    if (baseUrl != null) {
                        Manifest mf = readManifest(baseUrl);
                        if (mf != null) {
                            definePackage(pname, mf, baseUrl);
                            return;
                        }
                    }
                    definePackage(pname, null, null, null, null, null, null, null);
                }
            }
        }

        private Manifest readManifest(URL baseUrl) {
            try {
                URL umf = new URL(baseUrl + "META-INF/MANIFEST.MF");
                InputStream is = umf.openStream();
                Manifest mf = new Manifest(is);
                is.close();
                return mf;
            } catch (Exception e) {
                return null;
            }
        }

        private URL getBaseURL(URL url, String classname) {
            try {
                String u = url.toExternalForm();
                if (url.getQuery() != null) {
                    u = u.substring(0, u.lastIndexOf('?'));
                }
                u = u.substring(0, u.length() - (classname + ".class").length());
                return new URL(u);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        protected Package definePackage(String name, Manifest man, URL url)
                throws IllegalArgumentException {
            String path = name.replace('.', '/').concat("/");
            String specTitle = null, specVersion = null, specVendor = null;
            String implTitle = null, implVersion = null, implVendor = null;
            String sealed = null;
            URL sealBase = null;

            Attributes attr = man.getAttributes(path);
            if (attr != null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
                sealed = attr.getValue(Name.SEALED);
            }
            attr = man.getMainAttributes();
            if (attr != null) {
                if (specTitle == null) {
                    specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
                }
                if (specVersion == null) {
                    specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
                }
                if (specVendor == null) {
                    specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
                }
                if (implTitle == null) {
                    implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
                }
                if (implVersion == null) {
                    implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
                }
                if (implVendor == null) {
                    implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
                }
                if (sealed == null) {
                    sealed = attr.getValue(Name.SEALED);
                }
            }
//          no sealing
//			if ("true".equalsIgnoreCase(sealed)) {
//				sealBase = url;
//			}
            return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        }

        private synchronized ProtectionDomain getProtectionDomain(URL url) {
            try {
                if ("jar".equals(url.getProtocol())) {
                    String jarPath = url.getPath();
                    jarPath = jarPath.substring(0, jarPath.lastIndexOf('!'));
                    URL jarUrl;
                    if (shouldDecorateURLs()) {
                        jarUrl = new URL(jarPath + "?isolate=" + name);
                    }
                    else {
                        jarUrl = new URL(jarPath);
                    }
                    ProtectionDomain domain = domainCache.get(jarUrl);
                    if (domain == null) {
                        domain = new ProtectionDomain(
                                new CodeSource(jarUrl, new Certificate[0]),
                                isolateDomain.getPermissions());
                        domainCache.put(jarUrl, domain);
                    }
                    return domain;
                }
                else {
                    return isolateDomain;
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "IsolateClassloader[" + name + "]";
        }
    }

    private static class AnyThrow {

        public static void throwUncheked(Throwable e) {
            AnyThrow.<RuntimeException>throwAny(e);
        }

        @SuppressWarnings("unchecked")
        private static <E extends Throwable> void throwAny(Throwable e) throws E {
            throw (E)e;
        }
    }

    // TODO make wrapper print stream shared utility class
    private static class WrapperPrintStream extends FilterOutputStream {

        private String prefix;
        private PrintStream printStream;
        private ByteArrayOutputStream buffer;
        private boolean silenced;

        public WrapperPrintStream(String prefix, PrintStream printStream) {
            super(printStream);
            this.prefix = prefix;
            this.printStream = printStream;
            this.buffer = new ByteArrayOutputStream();
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public void setSilenced(boolean silenced) {
            this.silenced = silenced;
        }

        private void dumpBuffer() throws IOException {
            if (!silenced) {
                printStream.append(prefix);
                printStream.write(buffer.toByteArray());
                printStream.flush();
            }
            buffer.reset();
        }

        @Override
        public synchronized void write(int c) throws IOException {
            synchronized(printStream) {
                buffer.write(c);
                if (c == '\n') {
                    dumpBuffer();
                }
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            synchronized(printStream) {
                for (int i = 0; i != len; ++i) {
                    if (b[off + i] == '\n') {
                        writeByChars(b, off, len);
                        return;
                    }
                }
                buffer.write(b, off, len);
            }
        }

        private void writeByChars(byte[] cbuf, int off, int len) throws IOException {
            for (int i = 0; i != len; ++i) {
                write(cbuf[off + i]);
            }
        }

        @Override
        public void close() throws IOException {
            super.flush();
            dumpBuffer();
        }
    }

    private static abstract class PrintStreamMultiplexor extends PrintStream {

        protected ThreadLocal<Boolean> INSIDE = new ThreadLocal<Boolean>();

        protected abstract PrintStream resolve();

        public PrintStreamMultiplexor() {
            super(new ByteArrayOutputStream(8));
        }

        @Override
        public void write(byte[] b) throws IOException {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.write(b);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void flush() {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.flush();
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void close() {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.close();
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public boolean checkError() {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.checkError();
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void write(int b) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.write(b);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.write(buf, off, len);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(boolean b) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(b);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(char c) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(c);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(int i) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(i);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(long l) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(l);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(float f) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(f);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(double d) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(d);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(char[] s) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(s);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(String s) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(s);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void print(Object obj) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.print(obj);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println() {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println();
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(boolean x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(char x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(int x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(long x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(float x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(double x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(char[] x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(String x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public void println(Object x) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                resolve.println(x);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public PrintStream printf(String format, Object... args) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.printf(format, args);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public PrintStream printf(Locale l, String format, Object... args) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.printf(l, format, args);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public PrintStream format(String format, Object... args) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.format(format, args);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public PrintStream format(Locale l, String format, Object... args) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.format(l, format, args);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public PrintStream append(CharSequence csq) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.append(csq);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public PrintStream append(CharSequence csq, int start, int end) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.append(csq, start, end);
            }
            finally {
                INSIDE.set(false);
            }
        }

        @Override
        public PrintStream append(char c) {
            PrintStream resolve = resolve();
            INSIDE.set(true);
            try {
                return resolve.append(c);
            }
            finally {
                INSIDE.set(false);
            }
        }
    }

    @SuppressWarnings("serial")
    private static abstract class PropertiesMultiplexor extends Properties {

        protected abstract Properties resolve();

        @Override
        public Object setProperty(String key, String value) {
            return resolve().setProperty(key, value);
        }

        @Override
        public void load(Reader reader) throws IOException {
            resolve().load(reader);
        }

        @Override
        public int size() {
            return resolve().size();
        }

        @Override
        public boolean isEmpty() {
            return resolve().isEmpty();
        }

        @Override
        public Enumeration<Object> keys() {
            return resolve().keys();
        }

        @Override
        public Enumeration<Object> elements() {
            return resolve().elements();
        }

        @Override
        public boolean contains(Object value) {
            return resolve().contains(value);
        }

        @Override
        public boolean containsValue(Object value) {
            return resolve().containsValue(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return resolve().containsKey(key);
        }

        @Override
        public Object get(Object key) {
            return resolve().get(key);
        }

        @Override
        public void load(InputStream inStream) throws IOException {
            resolve().load(inStream);
        }

        @Override
        public Object put(Object key, Object value) {
            return resolve().put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return resolve().remove(key);
        }

        @Override
        public void putAll(Map<? extends Object, ? extends Object> t) {
            resolve().putAll(t);
        }

        @Override
        public void clear() {
            resolve().clear();
        }

        @Override
        public Object clone() {
            return resolve().clone();
        }

        @Override
        public String toString() {
            return resolve().toString();
        }

        @Override
        public Set<Object> keySet() {
            return resolve().keySet();
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return resolve().entrySet();
        }

        @Override
        public Collection<Object> values() {
            return resolve().values();
        }

        @Override
        public boolean equals(Object o) {
            return resolve().equals(o);
        }

//		@SuppressWarnings("deprecation")
        @Override
        public void save(OutputStream out, String comments) {
            resolve().save(out, comments);
        }

        @Override
        public int hashCode() {
            return resolve().hashCode();
        }

        @Override
        public void store(Writer writer, String comments) throws IOException {
            resolve().store(writer, comments);
        }

        @Override
        public void store(OutputStream out, String comments) throws IOException {
            resolve().store(out, comments);
        }

        @Override
        public void loadFromXML(InputStream in) throws IOException,
                InvalidPropertiesFormatException {
            resolve().loadFromXML(in);
        }

        @Override
        public void storeToXML(OutputStream os, String comment)
                throws IOException {
            resolve().storeToXML(os, comment);
        }

        @Override
        public void storeToXML(OutputStream os, String comment, String encoding)
                throws IOException {
            resolve().storeToXML(os, comment, encoding);
        }

        @Override
        public String getProperty(String key) {
            return resolve().getProperty(key);
        }

        @Override
        public String getProperty(String key, String defaultValue) {
            return resolve().getProperty(key, defaultValue);
        }

        @Override
        public Enumeration<?> propertyNames() {
            return resolve().propertyNames();
        }

        @Override
        public Set<String> stringPropertyNames() {
            return resolve().stringPropertyNames();
        }

        @Override
        public void list(PrintStream out) {
            resolve().list(out);
        }

        @Override
        public void list(PrintWriter out) {
            resolve().list(out);
        }
    }
}
