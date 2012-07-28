package org.gridkit.util.vicontrol.isolate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.util.vicontrol.MassExec;
import org.gridkit.util.vicontrol.ViConfigurable;
import org.gridkit.util.vicontrol.ViNode;
import org.gridkit.util.vicontrol.ViNodeConfig;
import org.gridkit.util.vicontrol.VoidCallable;
import org.gridkit.util.vicontrol.VoidCallable.VoidCallableWrapper;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class IsolateViNode implements ViNode {

	private ViNodeConfig config = new ViNodeConfig();

	private Isolate isolate;
	private ViConfigurable configProxy;
	
	private boolean destroyed;
	
	private static AtomicInteger COUNTER = new AtomicInteger();
	
	public IsolateViNode() {
	}
	
	public IsolateViNode(String name) {
		setName(this, name);
	}
	
	@Override
	public String getProp(String propName) {
		if (isolate == null) {
			return config.getProp(propName);
		}
		else {
			return isolate.getProp(propName);
		}
	}

	@Override
	public synchronized void setProp(String propName, String value) {
		ensureNotDestroyed();
		config.setProp(propName, value);
		if (isolate != null) {
			configProxy.setProp(propName, value);
		}
	}
	
	@Override
	public synchronized void setProps(Map<String, String> props) {
		ensureNotDestroyed();
		config.setProps(props);
		if (isolate != null) {
			configProxy.setProps(props);			
		}
	}
	
	@Override
	public synchronized void addStartupHook(String name, Runnable hook, boolean override) {
		ensureNotDestroyed();
		if (isolate != null) {
			throw new IllegalStateException("already started");
		}
		config.addStartupHook(name, hook, override);
	}

	@Override
	public synchronized void addShutdownHook(String name, Runnable hook, boolean override) {
		ensureNotDestroyed();
		config.addShutdownHook(name, hook, override);
		if (isolate != null) {
			throw new IllegalStateException("already started");
		}
	}

	@Override
	public synchronized void suspend() {
		ensureStarted();
		isolate.suspend();
	}

	@Override
	public void resume() {
		ensureStarted();
		isolate.resume();
	}

	@Override
	public synchronized void shutdown() {
		if (isolate != null && !destroyed) {
			destroy();
		}
		else {
			destroyed = true;
		}
	}

	@Override
	public void exec(Runnable task) {
		ensureStarted();
		resolve(isolate.submit(task));
	}

	@Override
	public void exec(VoidCallable task) {
		ensureStarted();
		resolve(isolate.submit(new VoidCallableWrapper(task)));
	}

	@Override
	public <T> T exec(Callable<T> task) {
		ensureStarted();
		return resolve(isolate.submit(task));
	}

	@Override
	public Future<Void> submit(Runnable task) {
		ensureStarted();
		return isolate.submit(task);
	}

	@Override
	public Future<Void> submit(VoidCallable task) {
		ensureStarted();
		return isolate.submit(new VoidCallableWrapper(task));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ensureStarted();
		return isolate.submit(task);
	}

	@Override
	public <T> List<T> massExec(Callable<? extends T> task) {
		ensureStarted();
		return MassExec.singleNodeMassExec(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(Runnable task) {
		ensureStarted();
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(VoidCallable task) {
		ensureStarted();
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		ensureStarted();
		return MassExec.singleNodeMassSubmit(this, task);
	}

	public Isolate getIsolate() {
		ensureStarted();
		return isolate;
	}
	
	private void ensureNotDestroyed() {
		if (destroyed) {
			throw new IllegalArgumentException("Isolate was destroyed");
		}		
	}
	
	private <V> V resolve(Future<V> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted");
		} catch (ExecutionException e) {
			Throwable cause = (Throwable) e.getCause();
			
			StackTraceElement receiverMarker = new StackTraceElement(Isolate.class.getName(), "", null, -1);
			StackTraceElement donorMarker = new StackTraceElement(IsolateViNode.class.getName(), "resolve", null, -1);
			StackTraceElement boundary = new StackTraceElement("<isolate-boundary>", "<exec>", isolate.getName(), -1);
			
			ExceptionWeaver.replaceStackTop(cause, receiverMarker, new Exception(), donorMarker, boundary);
			AnyThrow.throwUncheked(cause); // TODO stack trace weaving
			throw new Error("Unreachable");
		}
	}
	
	private synchronized void ensureStarted() {
		ensureNotDestroyed();
		if (isolate == null) {
			String name = config.getProp(IsolateProps.NAME, "ISOLATE@" + COUNTER.getAndIncrement());
			isolate = new Isolate(name);
			configProxy = new ConfigProxy();
			isolate.start();
			config.apply(configProxy);
		}
	}
	
	private synchronized void destroy() {
		config.apply(new ViConfigurable() {
			
			@Override
			public void setProps(Map<String, String> props) {
				// ignore
			}
			
			@Override
			public void setProp(String propName, String value) {
				// ignore
			}
			
			@Override
			public void addStartupHook(String name, Runnable hook, boolean override) {
				// ignore
			}
			
			@Override
			public void addShutdownHook(String name, Runnable hook, boolean override) {
				isolate.exec(hook);
			}
		});
		isolate.stop();
		destroyed = true;
	}

	private class ConfigProxy implements ViConfigurable {

		@Override
		public void setProp(String propName, String value) {
			if (value == null) {
				isolate.setProp(propName, value);
			}
			else {
				if (propName.startsWith("isolate:")) {
					if (propName.equals(IsolateProps.NAME)) {
						isolate.setName(value);
					}
					else if (propName.startsWith(IsolateProps.PACKAGE)) {
						String pn = propName.substring(IsolateProps.PACKAGE.length());
						isolate.addPackage(pn);
					}
					else if (propName.startsWith(IsolateProps.SHARED)) {
						String cn = propName.substring(IsolateProps.SHARED.length());
						isolate.exclude(cn);
					}
					else if (propName.startsWith(IsolateProps.CP_ADD)) {
						try {
							if (value.length() == 0) {
								value = propName.substring(IsolateProps.CP_ADD.length());
							}
							isolate.addToClasspath(new URL(value));
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
					else if (propName.startsWith(IsolateProps.CP_REMOVE)) {
						try {
							if (value.length() == 0) {
								value = propName.substring(IsolateProps.CP_REMOVE.length());
							}
							isolate.removeFromClasspath(new URL(value));
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
					else {
						throw new IllegalArgumentException("Unknown isolate config directive [" + propName + "]");
					}
				}
				else {
					isolate.setProp(propName, value);
				}
			}
		}
	
		@Override
		public void setProps(Map<String, String> props) {
			for(Map.Entry<String, String> e: props.entrySet()) {
				setProp(e.getKey(), e.getValue());
			}
		}
	
		@Override
		public void addStartupHook(String name, Runnable hook, boolean override) {
			isolate.exec(hook); 
		}
	
		@Override
		public void addShutdownHook(String name, Runnable hook, boolean override) {
			// do nothing
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

	public static void setName(ViConfigurable node, String name) {
		node.setProp(IsolateProps.NAME, name);
	}
	
	public static void includePackage(ViConfigurable node, String pkg) {
		node.setProp(IsolateProps.PACKAGE + pkg, "");
	}

	public static void includePackages(ViConfigurable node, String... packages) {
		for(String pkg: packages) {
			includePackage(node, pkg);
		}
	}
	
	public static void excludeClass(ViConfigurable node, Class<?> type) {
		if (type.isPrimitive() || type.isArray() || type.getDeclaringClass() != null) {
			throw new IllegalArgumentException("Non inner, non primity, non array class is expected");
		}
		node.setProp(IsolateProps.SHARED + type.getName(), "");
	}	
	
	public static void addToClasspath(ViConfigurable node, URL url) {
		node.setProp(IsolateProps.CP_ADD + url.toString(), url.toString());
	}

	public static void removeFromClasspath(ViConfigurable node, URL url) {
		node.setProp(IsolateProps.CP_REMOVE + url.toString(), url.toString());
	}
}
