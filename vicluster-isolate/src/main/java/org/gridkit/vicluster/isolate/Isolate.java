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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.RmiGateway;

/**
 *	@author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class Isolate implements AdvancedExecutor {
	
	static boolean VERBOSE_CLASSES = false;
	
	private static final InheritableThreadLocal<Isolate> ISOLATE = new InheritableThreadLocal<Isolate>();
	
	private static PrintStream rootOut;
	private static PrintStream rootErr;
	private static Properties rootProperties;
	
	static {
		
		// initializing LogManager outside of isolate
		LogManager.getLogManager();
		
		System.err.println("Installing java.lang.System multiplexor");
		
		rootOut = System.out;
		rootErr = System.err;
		rootProperties = System.getProperties();
		
		PrintStream mOut = new PrintStreamMultiplexor() {
			@Override
			protected PrintStream resolve() {
				Isolate i = ISOLATE.get();
				if (i == null) {
					return rootOut;
				}
				else {
					return i.stdOut;
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
					return i.stdErr;
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

	public static Isolate currentIsolate() {
		return ISOLATE.get();
	}
	
	private static long MIN_THREAD_SCAN_INTERVAL = TimeUnit.MILLISECONDS.toNanos(100);
	
	private final String name;
	private IsolateThreadGroup threadGroup;
	private IsolatedClassloader cl;
	private long lastThreadScan = System.nanoTime();
	
	private PrintStream stdOut;
	private PrintStream stdErr;
	private WrapperPrintStream wrpOut;
	private WrapperPrintStream wrpErr;
	private Properties sysProps;
	private int shutdownRetry = 0;
	
	private IsolateClassTransformer transformer;

	private List<ThreadKiller> threadKillers = new ArrayList<ThreadKiller>();
	
	private RmiFacility outside;
	private RmiFacility inside;
	
	private boolean terminated = false;

	// TODO settle what thread-pool we need
	private ExecutorService threadPool;
	
	public Isolate(String name, String... packages) {		
		this.name = name;
		this.cl = new IsolatedClassloader(getClass().getClassLoader());
		
		for(String p: packages) {
			this.cl.addPackage(p);
		}

		threadGroup = new IsolateThreadGroup(name);
		
		sysProps = new Properties();
		sysProps.putAll(System.getProperties());
		sysProps.put("isolate.name", name);
		
		wrpOut = new WrapperPrintStream("[" + name + "] ", rootOut);
		stdOut = new PrintStream(wrpOut);
		wrpErr = new WrapperPrintStream("[" + name + "] ", rootErr);
		stdErr = new PrintStream(wrpErr);
		
		threadPool = Executors.newCachedThreadPool(threadGroup);
	}
	
	private void initRmi() {
		
		ClassLoader inCl = cl;
		ClassLoader outCl = cl.getBaseClassLoader();
		
		SyncBlobPipe sideA = new SyncBlobPipe();
		SyncBlobPipe sideB = new SyncBlobPipe();
		
		sideA.bind(sideB);
		
		Executor outExec = new Executor() {			
			@Override
			public void execute(Runnable command) {
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(cl.getParent());
				try {
					command.run();
				}
				finally {
					Thread.currentThread().setContextClassLoader(cl);
				}
			}
		};
		
		Executor inExec = new Executor() {
			@Override
			public void execute(Runnable command) {
				threadPool.execute(new IsolateTaskMarker(command));
			}
		};

		outside = (RmiFacility) new IsolateRmiFacility();
		outside.startRmi(name + "-outbound", outCl, sideB, outExec);		

		try {
			Constructor<?> cc = inCl.loadClass(IsolateRmiFacility.class.getName()).getDeclaredConstructor();
			cc.setAccessible(true);
			inside = (RmiFacility) cc.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		inside.startRmi(name + "-inbound", inCl, sideA, inExec);
		
	}

	public String getName() {
		return name;
	}
	
	public PrintStream getStdOur() {
		return stdOut;
	}

	public PrintStream getStdErr() {
		return stdErr;
	}
	
	public synchronized void setClassTransformer(IsolateClassTransformer transformer) {
		if (this.transformer != null) {
			throw new IllegalStateException("Transformer is already defined");
		}
		this.transformer = transformer;
		this.transformer.init(this, new TransformerSupport());
	}
	
	public synchronized void addThreadKiller(ThreadKiller killer) {
		threadKillers.add(killer);
	}
	
	public synchronized void addPackage(String packageName) {
		checkInternalPackage(packageName);
		cl.addPackage(packageName);
	}
	
	public void checkInternalPackage(String name) {
		if (inside != null) {
			if (FutureEx.class.getName().startsWith(name)
				|| RmiGateway.class.getName().startsWith(name)) {
				throw new IllegalArgumentException("Package '" + name + "' is used interally, you can add/remove it to isolate only before first usage");
			}
		}
	}
	
	/**
	 * Classes marked as "excluded" will always be loaded from parent class loader.
	 */
	public synchronized void exclude(String exclude) {
		checkInternalPackage(exclude.substring(0, exclude.lastIndexOf('.')));
		cl.exclude(exclude);
	}

	/**
	 * Classes marked as "excluded" will always be loaded from parent class loader.
	 */
	public synchronized void exclude(Class<?>... excludes) {
		cl.exclude(excludes);
	}

	/**
	 * Prohibit loading classes or resources from given URL via parent class loader.
	 */
	public synchronized void removeFromClasspath(URL basePath) {
		cl.prohibitFromClasspath(basePath);
	}

	/**
	 * Add URL to isolate class path.
	 */
	public synchronized void addToClasspath(URL path) {
		cl.addToClasspath(path);
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

	public synchronized void exec(Runnable task) {
		try {
			submit(task).get();
		}
		catch(ExecutionException e) {
			weaveAndRethrow(e.getCause());
		}
		catch(InterruptedException e) {
			AnyThrow.throwUncheked(e);
		}
	}

	public synchronized <V> V exec(Callable<V> task) {
		try {
			return submit(task).get();
		}
		catch(ExecutionException e) {
			weaveAndRethrow(e.getCause());
			throw new Error("Unreachable");
		}
		catch(InterruptedException e) {
			AnyThrow.throwUncheked(e);
			throw new Error("Unreachable");
		}
	}
	
	private synchronized RmiFacility ensureRmi() {
		if (inside == null) {
			initRmi();
		}
		return outside;
	}
	
	private synchronized void stopRmi() {
		if (inside != null) {
			inside.stop();
		}
		if (outside != null) {
			outside.stop();
		}
	}
	
	@Override
	public void execute(Runnable task) {
		ensureRmi().submit(task);
	}

	@Override
	public FutureEx<Void> submit(Runnable task) {
		return new IsolateFutures.FutureUnproxy<Void>(ensureRmi().submit(task));
	}

	@Override
	public <V> FutureEx<V> submit(Callable<V> task) {
		return new IsolateFutures.FutureUnproxy<V>(ensureRmi().submit(task));
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
	
	@SuppressWarnings("deprecation")
	public void suspend() {
		threadGroup.suspend();
	}

	@SuppressWarnings("deprecation")
	public void resume() {
		threadGroup.resume();
	}
	
	public synchronized void stop() {
		if (terminated) {
			return;
		}
		else {
			terminated = true;
		}
 		
		stopRmi();
		
		try {		
			threadPool.shutdown();
			// give thread pool a chance to graceful shutdown itself
			threadPool.awaitTermination(100, TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException e) {
			// ignore
		}
		threadPool.shutdownNow();
		threadPool = null;
		
		stdErr.println("Stopping ...");
		while(true) {
			while( 0 < kill(threadGroup)  
					+ removeShutdownHooks()
					+ removeAppContexts() ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				++shutdownRetry;
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
				stdErr.println("Isolate clean up failed, some garbage may remain");
				break;
			}
		}
		cl = null;
		threadGroup = null;
		sysProps = new Properties(); // make props empty
		
		stdErr.println("Stopped");
		
		System.gc();
		Runtime.getRuntime().runFinalization();
	}
	
	@SuppressWarnings("deprecation")
	private int kill(ThreadGroup tg) {
		int threadCount = 0;
		
		Thread[] threads = new Thread[2 * tg.activeCount()];
		int n = tg.enumerate(threads);
		for(int i = 0; i != n; ++i) {
			++threadCount;
			Thread t = threads[i];
			if (Runtime.getRuntime().removeShutdownHook(t)) {
				stdErr.println("Removing shutdown hook: " + t.getName());
			}
			if (t.getState() != State.TERMINATED) {
				stdErr.println("Killing: " + t.getName());
				try { t.resume(); }	catch(Exception e) {/* ignore */};
				try { t.start(); }	catch(Exception e) {/* ignore */};
				try { t.interrupt(); }	catch(Exception e) {/* ignore */};
				try { t.stop(new ThreadDoomError()); }	catch(IllegalStateException e) {/* ignore */};				
			}
			else {
				if (shutdownRetry % 10 == 9) {
					stdErr.println("Already terminated: " + t.getName());
				}
			}
			
			if (t.isAlive() && shutdownRetry > 24) {
				if (shutdownRetry > 10 && (shutdownRetry % 10 == 5)) {
					StackTraceElement[] trace = t.getStackTrace();
					for(StackTraceElement e: trace) {
						stdErr.println("  at " + e);
					}
				}
				try {
					try { t.interrupt(); }	catch(Exception e) {/* ignore */};
					tryPlugableKillers(t);
					try { t.interrupt(); }	catch(Exception e) {/* ignore */};
					try { t.stop(new ThreadDoomError()); }	catch(IllegalStateException e) {/* ignore */};				
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
	
	private void tryPlugableKillers(Thread t) {
		for (ThreadKiller killer: threadKillers) {
			killer.tryToKill(this, t);
		}
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
				if (Runtime.getRuntime().removeShutdownHook(t)) {
					stdErr.println("Removing shutdown hook: " + t.getName());
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
			e.printStackTrace();
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

	@SuppressWarnings("serial")
	private class ThreadDoomError extends ThreadDeath {

		@Override
		public Throwable getCause() {
			return null;
		}

		@Override
		public String toString() {
			return "Isolate [" + name + "] has been terminated";
		}

		@Override
		public void printStackTrace() {
		}

		@Override
		public void printStackTrace(PrintStream s) {
		}

		@Override
		public void printStackTrace(PrintWriter s) {
		}

		@Override
		public StackTraceElement[] getStackTrace() {
			return null;
		}
	}
	
	private class IsolateThreadGroup extends ThreadGroup implements ThreadFactory {
		
		int n = 0;
		
		private IsolateThreadGroup(String name) {
			super(name);
		}

		@Override
		public synchronized Thread newThread(final Runnable r) {
			Runnable ti = new Runnable() {
				
				@Override
				public void run() {
					ISOLATE.set(Isolate.this);
					r.run();
				}
			};
			Thread th = new Thread(this, ti, "worker-" + n);
			th.setDaemon(true);
			th.setContextClassLoader(cl);
			return th;
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

	private static class IsolateTaskMarker implements Runnable {
		
		private Runnable runnable;
		
		public IsolateTaskMarker(Runnable runnable) {
			this.runnable = runnable;
		}
		
		@Override
		public void run() {
			runnable.run();
		}
	}
	
	private class IsolatedClassloader extends ClassLoader {
		
		private ClassLoader baseClassloader;
		private List<String> packages;
		private Set<String> excludes;
		
		private Collection<String> forbidenPaths = new ArrayList<String>();
		private Collection<URL> externalPaths = new ArrayList<URL>();
		private URLClassLoader cpExtention;
		
		private ProtectionDomain isolateDomain;
		private Map<URL, ProtectionDomain> domainCache = new HashMap<URL, ProtectionDomain>();
		
		IsolatedClassloader(ClassLoader base) {
			super(null);			
			this.baseClassloader = base;
			this.packages = new ArrayList<String>();
			this.excludes = new HashSet<String>();

			PermissionCollection pc = new Permissions();
			pc.add(new AllPermission());
			
			ProtectionDomain domain = new ProtectionDomain(
					/* codesource - */ getClass().getProtectionDomain().getCodeSource(),
					/* permissions - */ pc,
					/* classloader - */ null,
					/* principals - */ getClass().getProtectionDomain().getPrincipals());
			this.isolateDomain = domain;
		}
		
		public ClassLoader getBaseClassLoader() {
			return baseClassloader;
		}
		
		public void addPackage(String prefix) {
			packages.add(prefix);
		}

		public void exclude(String className) {
			excludes.add(className);
		}
		
		/** 
		 * "Excluded" classes are always to be loaded from parent ClassLoader 
		 */
		public void exclude(Class<?>... excludedClasses) {
			for (Class<?> clazz : excludedClasses) {
				excludes.add(clazz.getName());
			}
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
		
		public void clearAssertionStatus() {
			baseClassloader.clearAssertionStatus();
		}

		public synchronized URL getResource(String name) {
			if (cpExtention == null) {
				cpExtention = new URLClassLoader(externalPaths.toArray(new URL[0]));
			}
			URL r = cpExtention.findResource(name);
			if (r != null) {
				return r;
			}
			r = baseClassloader.getResource(name);
			if (r == null || isForbiden(r)) {
				return null;
			}
			else {
				return r;
			}
		}

		public synchronized Enumeration<URL> getResources(String name) throws IOException {
			if (cpExtention == null) {
				cpExtention = new URLClassLoader(externalPaths.toArray(new URL[0]));
			}
			Vector<URL> result = new Vector<URL>();
			// TODO my have several names
			URL r = cpExtention.findResource(name);
			if (r != null) {
				result.add(r);
			}
			
			Enumeration<URL> en = baseClassloader.getResources(name);
			
			while(en.hasMoreElements()) {
				r = en.nextElement();
				if (!isForbiden(r)) {
					result.add(r);
				}
			}
			
			return result.elements();
		}

		private boolean isForbiden(URL r) {
			if (r != null && !forbidenPaths.isEmpty()) {
				String s = r.toString();
				for(String path: forbidenPaths) {
					if (s.startsWith(path)) {
						return true;
					}
				}
			}
			return false;
		}
		
		public void setClassAssertionStatus(String className, boolean enabled) {
			baseClassloader.setClassAssertionStatus(className, enabled);
		}

		public void setDefaultAssertionStatus(boolean enabled) {
			baseClassloader.setDefaultAssertionStatus(enabled);
		}

		public void setPackageAssertionStatus(String packageName, boolean enabled) {
			baseClassloader.setPackageAssertionStatus(packageName, enabled);
		}

		Class<?> loadIsolated(String name) throws ClassNotFoundException {
			if (isExcluded(name)) {
				throw new ClassNotFoundException("Cannot isolated class " + name);
			}
			else {
				Class<?> cl = findLoadedClass(name);
				if (cl == null) {
					if (cl == null) {
						cl = findClass(name);
					}
					if (cl == null) {
						throw new ClassNotFoundException(name);
					}
				}
				return cl;					
			}
		}

		Class<?> loadClassFromBytes(String name, byte[] data) throws ClassNotFoundException {
			if (isExcluded(name)) {
				throw new ClassNotFoundException("Cannot isolated class " + name);
			}
			else {
				Class<?> cl = findLoadedClass(name);
				if (cl == null) {
					if (cl == null) {
						cl = defineClass(name, data, 0, data.length);			
						if (VERBOSE_CLASSES) {
							stdOut.println(name + " loaded in isolate (from bytes)");
						}
					}
					if (cl == null) {
						throw new ClassNotFoundException(name);
					}
				}
				return cl;					
			}
		}
		
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (!isExcluded(name)) {
				for(String prefix: packages) {
					if (name.startsWith(prefix + ".")) {
						return loadIsolated(name);
					}
				}
			}
			if (name.equals("sun.awt.AppContext")) {
				new Exception("loading AppContext").printStackTrace();
			}
			if (VERBOSE_CLASSES) {
				stdOut.println("[" + Isolate.this.name + "] " + name + " loaded from parent");
			}
			Class<?> cc = baseClassloader.loadClass(name);
			return cc;
		}
		
		private boolean isInternallyShared(String name) {
			if (name.equals(Isolate.class.getName()) 
					|| name.startsWith(Isolate.class.getName() + "$")
					|| name.equals(ThreadKiller.class.getName())
//					|| name.startsWith("org.gridkit.zerormi.")
					) {
				return true;
			}
			else {
				return false;
			}
		}
		
		private boolean isExcluded(String name) {	
			if (isInternallyShared(name)) {
				return true;
			}
			else {
				String topClass = name;
				int n = topClass.indexOf('$');
				if (n >= 0) {
					topClass = topClass.substring(0, n);
				}
				return excludes.contains(topClass);
			}
		}

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
				byte[] cd = asBytes(res);
				try {					
					return defineIsolatedClass(classname, url, cd);
				}
				catch(OutOfMemoryError e) {
					// try it again once
					System.gc();
					System.runFinalization();
					System.gc();
					return defineIsolatedClass(classname, url, cd);
				}
			}
			catch(Exception e) {
				throw new ClassNotFoundException(classname);
			}
		}

		private Class<?> defineIsolatedClass(String classname, URL url, byte[] cd) throws ClassFormatError {
			if (transformer != null) {
				@SuppressWarnings("unused") // just for debuging
				byte[] ocd = cd; 
				cd = transformer.transform(this, classname, cd); 
				if (VERBOSE_CLASSES) {
					stdOut.println(classname + " transformed");
				}
			}
			Class<?> cl = defineClass(url, classname, cd);			
			if (VERBOSE_CLASSES) {
				stdOut.println(classname + " loaded in isolate");
			}
			return cl;
		}

		private Class<?> defineClass(URL url, String classname, byte[] cd) throws ClassFormatError {
			Class<?> c = defineClass(classname, cd, 0, cd.length, getProtectionDomain(url));
			return c;
		}

		private synchronized ProtectionDomain getProtectionDomain(URL url) {
			try {
				if ("jar".equals(url.getProtocol())) {
					String jarPath = url.getPath();
					jarPath = jarPath.substring(0, jarPath.lastIndexOf('!'));
					URL jarUrl = new URL(jarPath + "?isolate=" + name);
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
			return "IsolatedClassloader[" + name + "]";
		}
	}

	private class TransformerSupport implements IsolateClassTransformerSupport {

		@Override
		public Class<?> loadIsolated(String className) throws ClassNotFoundException {
			return cl.loadIsolated(className);
		}

		@Override
		public Class<?> defineIsolated(String className, byte[] classData) throws ClassNotFoundException {			
			return cl.loadClassFromBytes(className, classData);
		}

		@Override
		public boolean isAssignable(String targetClass, String questionClass) {
			// TODO not implement
			return targetClass.equals(questionClass);
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
	
	private static class WrapperPrintStream extends FilterOutputStream {

		private String prefix;
		private boolean startOfLine;
		private PrintStream printStream;
		
		public WrapperPrintStream(String prefix, PrintStream printStream) {
			super(printStream);
			this.prefix = prefix;
			this.startOfLine = true;
			this.printStream = printStream;
		}
		
		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}
		
		@Override
		public synchronized void write(int c) throws IOException {
			synchronized(printStream) {
				checkNewLine();
				if (c == '\n') {
					startOfLine = true;
				}
				super.write(c);
				if (startOfLine) {
					// flush after end of line
					super.flush();
				}
			}
		}

		private void checkNewLine() {
			if (startOfLine) {
				printStream.append(prefix);
				startOfLine = false;
			}
		}
	
		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			synchronized(printStream) {
				checkNewLine();
				for (int i = 0; i != len; ++i) {
					if (b[off + i] == '\n') {
						writeByChars(b, off, len);
						return;
					}
				}
				super.write(b, off, len);
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
		}
	}
	
	private static abstract class PrintStreamMultiplexor extends PrintStream {
		
		protected abstract PrintStream resolve();
		
		public PrintStreamMultiplexor() {
			super(new ByteArrayOutputStream(8));
		}
		
		public int hashCode() {
			return resolve().hashCode();
		}
		public void write(byte[] b) throws IOException {
			resolve().write(b);
		}
		public boolean equals(Object obj) {
			return resolve().equals(obj);
		}
		public String toString() {
			return resolve().toString();
		}
		public void flush() {
			resolve().flush();
		}
		public void close() {
			resolve().close();
		}
		public boolean checkError() {
			return resolve().checkError();
		}
		public void write(int b) {
			resolve().write(b);
		}
		public void write(byte[] buf, int off, int len) {
			resolve().write(buf, off, len);
		}
		public void print(boolean b) {
			resolve().print(b);
		}
		public void print(char c) {
			resolve().print(c);
		}
		public void print(int i) {
			resolve().print(i);
		}
		public void print(long l) {
			resolve().print(l);
		}
		public void print(float f) {
			resolve().print(f);
		}
		public void print(double d) {
			resolve().print(d);
		}
		public void print(char[] s) {
			resolve().print(s);
		}
		public void print(String s) {
			resolve().print(s);
		}
		public void print(Object obj) {
			resolve().print(obj);
		}
		public void println() {
			resolve().println();
		}
		public void println(boolean x) {
			resolve().println(x);
		}
		public void println(char x) {
			resolve().println(x);
		}
		public void println(int x) {
			resolve().println(x);
		}
		public void println(long x) {
			resolve().println(x);
		}
		public void println(float x) {
			resolve().println(x);
		}
		public void println(double x) {
			resolve().println(x);
		}
		public void println(char[] x) {
			resolve().println(x);
		}
		public void println(String x) {
			resolve().println(x);
		}
		public void println(Object x) {
			resolve().println(x);
		}
		public PrintStream printf(String format, Object... args) {
			return resolve().printf(format, args);
		}
		public PrintStream printf(Locale l, String format, Object... args) {
			return resolve().printf(l, format, args);
		}
		public PrintStream format(String format, Object... args) {
			return resolve().format(format, args);
		}
		public PrintStream format(Locale l, String format, Object... args) {
			return resolve().format(l, format, args);
		}
		public PrintStream append(CharSequence csq) {
			return resolve().append(csq);
		}
		public PrintStream append(CharSequence csq, int start, int end) {
			return resolve().append(csq, start, end);
		}
		public PrintStream append(char c) {
			return resolve().append(c);
		}
	}
	
	@SuppressWarnings("serial")
	private static abstract class PropertiesMultiplexor extends Properties {
		
		protected abstract Properties resolve();

		public Object setProperty(String key, String value) {
			return resolve().setProperty(key, value);
		}

		public void load(Reader reader) throws IOException {
			resolve().load(reader);
		}

		public int size() {
			return resolve().size();
		}

		public boolean isEmpty() {
			return resolve().isEmpty();
		}

		public Enumeration<Object> keys() {
			return resolve().keys();
		}

		public Enumeration<Object> elements() {
			return resolve().elements();
		}

		public boolean contains(Object value) {
			return resolve().contains(value);
		}

		public boolean containsValue(Object value) {
			return resolve().containsValue(value);
		}

		public boolean containsKey(Object key) {
			return resolve().containsKey(key);
		}

		public Object get(Object key) {
			return resolve().get(key);
		}

		public void load(InputStream inStream) throws IOException {
			resolve().load(inStream);
		}

		public Object put(Object key, Object value) {
			return resolve().put(key, value);
		}

		public Object remove(Object key) {
			return resolve().remove(key);
		}

		public void putAll(Map<? extends Object, ? extends Object> t) {
			resolve().putAll(t);
		}

		public void clear() {
			resolve().clear();
		}

		public Object clone() {
			return resolve().clone();
		}

		public String toString() {
			return resolve().toString();
		}

		public Set<Object> keySet() {
			return resolve().keySet();
		}

		public Set<Entry<Object, Object>> entrySet() {
			return resolve().entrySet();
		}

		public Collection<Object> values() {
			return resolve().values();
		}

		public boolean equals(Object o) {
			return resolve().equals(o);
		}

		@SuppressWarnings("deprecation")
		public void save(OutputStream out, String comments) {
			resolve().save(out, comments);
		}

		public int hashCode() {
			return resolve().hashCode();
		}

		public void store(Writer writer, String comments) throws IOException {
			resolve().store(writer, comments);
		}

		public void store(OutputStream out, String comments) throws IOException {
			resolve().store(out, comments);
		}

		public void loadFromXML(InputStream in) throws IOException,
				InvalidPropertiesFormatException {
			resolve().loadFromXML(in);
		}

		public void storeToXML(OutputStream os, String comment)
				throws IOException {
			resolve().storeToXML(os, comment);
		}

		public void storeToXML(OutputStream os, String comment, String encoding)
				throws IOException {
			resolve().storeToXML(os, comment, encoding);
		}

		public String getProperty(String key) {
			return resolve().getProperty(key);
		}

		public String getProperty(String key, String defaultValue) {
			return resolve().getProperty(key, defaultValue);
		}

		public Enumeration<?> propertyNames() {
			return resolve().propertyNames();
		}

		public Set<String> stringPropertyNames() {
			return resolve().stringPropertyNames();
		}

		public void list(PrintStream out) {
			resolve().list(out);
		}

		public void list(PrintWriter out) {
			resolve().list(out);
		}
	}
	
	/**
	 * Package visibility does not work between class loader.
	 * Have to make them public.
	 */
	public static interface FutureProxy<V> extends Future<V> {
		
		public void addListener(BoxProxy<? super V> box);
		
	}
	
	public static interface BoxProxy<V> {

		public void setData(V data);

		public void setError(Throwable e);		
	}
	
	/**
	 * Package visibility does not work between class loader.
	 * Have to make them public.
	 */
	public static interface RmiFacility {
		
		public void startRmi(String name, ClassLoader cl, BlobDuplex bd, Executor exec);
		
		public FutureProxy<Void> submit(Runnable task);

		public <V> FutureProxy<V> submit(Callable<V> task);
		
		public void stop();
	}
	
	/**
	 * Package visibility does not work between class loader.
	 * Have to make them public.
	 */
	public static interface BlobDuplex {
		
		public void bind(BlobSink receiver);
		
		public FutureProxy<Void> sendBinary(byte[] bytes);
		
		public void close();	
	}
	
	/**
	 * Package visibility does not work between class loader.
	 * Have to make them public.
	 */
	public static interface BlobSink {
		
		public FutureProxy<Void> blobReceived(byte[] blob);
		
		public void closed();
	}
	
	/** It should be declared as inner class to be shared between isolates */
	private static class SyncBlobPipe implements BlobDuplex {
		
		private SyncBlobPipe counterParty;
		private FutureBox<BlobSink> counterSink = new FutureBox<BlobSink>();

		public SyncBlobPipe() {			
		}
		
		public void bind(SyncBlobPipe cp) {
			if (counterParty != null || cp.counterParty != null) {
				throw new IllegalStateException("Already bound");
			}
			else {
				counterParty = cp;
				cp.counterParty = this;
			}
		}
		
		@Override
		public void bind(BlobSink receiver) {
			if (counterParty == null) {
				throw new IllegalStateException("Not bound");
				
			}
			counterParty.counterSink.setData(receiver);
		}
		
		private BlobSink other() {
			try {
				return counterSink.get();
			} catch (Exception e) {
				throw new RuntimeException();
			}			
		}
		
		@Override
		public FutureProxy<Void> sendBinary(final byte[] data) {
			if (!counterSink.isDone()) {
				final FutureBox<Void> ack = new FutureBox<Void>();
				counterSink.addListener(new Box<BlobSink>() {
					@Override
					public void setData(BlobSink receiver) {
						receiver.blobReceived(data).addListener(new IsolateFutures.BoxEnproxy<Void>(ack));					
					}
	
					@Override
					public void setError(Throwable e) {
						ack.setError(e);
					}
				});
				return new IsolateFutures.FutureEnproxy<Void>(ack);
			}
			else {
				return other().blobReceived(data);
			}
		}
				
		@Override
		public void close() {
			// ignore
			//other().closed();			
		}
	}
}