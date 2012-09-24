package org.gridkit.vicluster.spi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.util.concurrent.TaskService.Task;
import org.gridkit.vicluster.ViCloud2;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.spi.ViCloudExtention.DynNode;
import org.gridkit.vicluster.spi.ViCloudExtention.LazyMode;

class NanoCloud<V extends ViNode> implements ViCloud2<V> {

	private final Class<V> facade;
	
	private final CloudContext configContext = new CloudContext();
	
	private final Map<String, ManagedNode> nodes = new HashMap<String, NanoCloud<V>.ManagedNode>(); 

	private final Map<Class<?>, ViCloudExtention<?>> intf2ext = new LinkedHashMap<Class<?>, ViCloudExtention<?>>();
	private final Map<Method, Class<?>> method2ext = new HashMap<Method, Class<?>>();
	private final Map<Method, ViCloudExtention.LazyMode> method2mode = new HashMap<Method, ViCloudExtention.LazyMode>();
	
	private final TaskService taskService;
	
	public NanoCloud(Class<V> facade, Collection<ViCloudExtention<?>> extentions, TaskService taskService) {
		this.facade = facade;
		this.taskService = taskService;
		addExtention(new ViExecutorExtetion());
		if (extentions != null) {
			for(ViCloudExtention<?> ext: extentions) {
				addExtention(ext);
			}
		}
		compileMethodMap(facade);
	}
	
	private void addExtention(ViCloudExtention<?> ext) {
		intf2ext.put(ext.getFacadeInterface(), ext);		
	}

	private void compileMethodMap(Class<?> intf) {
		for(Method m: intf.getMethods()) {
			resolveMethod(m);
		}
		for(Class<?> sup: intf.getInterfaces()) {
			compileMethodMap(sup);
		}		
	}

	private void resolveMethod(Method m) {
		for(Class<?> extif: intf2ext.keySet()) {
			try {
				if (extif.getMethod(m.getName(), m.getParameterTypes()) != null) {
					method2ext.put(m, extif);
					method2mode.put(m, intf2ext.get(extif).modeForMethod(m));
					return;
				}
			} catch (SecurityException e) {
				AnyThrow.throwUncheked(e);
			} catch (NoSuchMethodException e) {
				continue;
			}
		}
		try {
			if (ViNode.class.getMethod(m.getName(), m.getParameterTypes()) != null) {
				method2ext.put(m, null);
				method2mode.put(m, LazyMode.NO_SPI_NEEDED);
				return;
			}
		} catch (SecurityException e) {
			AnyThrow.throwUncheked(e);
		} catch (NoSuchMethodException e) {
			// ok
		}
		throw new IllegalArgumentException("Cannot find extension for " + m);
	}

	public void applyConfig(CloudConfigSet configSet) {
		((RuleSet)configSet).apply(configContext);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public synchronized V byName(String pattern) {
		// TODO
		ManagedNode mn = ensureNode(pattern);
		return (V)mn.getProxy();
	}
	
	private boolean isPattern(String namePattern) {		
		return namePattern.indexOf('*') >= 0 || namePattern.indexOf('?') >= 0;
	}	

	private synchronized ManagedNode ensureNode(String name) {
		if (!nodes.containsKey(name)) {
			ManagedNode mn = new ManagedNode(name);
			Object proxy = Proxy.newProxyInstance(facade.getClassLoader(), new Class[]{facade}, mn);
			mn.proxy = (ViNode) proxy;
			nodes.put(name, mn);
		}
		return nodes.get(name);
	}
	
	@Override
	public synchronized V byLabel(String label) {
		return null;
	}

	@Override
	public List<V> listByName(String pattern) {
		return null;
	}

	@Override
	public List<V> listByLabel(String label) {
		return null;
	}

	@Override
	public void shutdown() {
	}
	
	private class InitSpiTask implements Task {

		ManagedNode node;
		AttrList config;
		
		public InitSpiTask(ManagedNode node, AttrList config) {
			this.node = node;
			this.config = config;
		}

		@Override
		public void run() {
			ViNodeSpi coreNode;
			try {
				Selector s = Selectors.name(node.name, ViNodeSpi.class.getName());
				configContext.ensureResource(s, config);
				coreNode = configContext.getNamedInstance(node.name, ViNodeSpi.class);
			}
			catch(Exception e) {
				synchronized(node.proxy) {
					node.futureNode.setError(e);
					node.proxy.notifyAll();
				}
				return;
			}
			synchronized(node.proxy) {
				node.node = coreNode;
				node.futureNode.setData(coreNode);
				node.proxy.notifyAll();
			}
		}

		@Override
		public void interrupt(Thread taskThread) {
			taskThread.interrupt();			
		}

		@Override
		public void cancled() {
			node.futureNode.setError(new CancellationException());
		}
	}
	
	
	private class ManagedNode implements ViNode, DynNode, InvocationHandler {

		private final String name;
		
		private ViNode proxy;
		
		private boolean pending;
		private FutureBox<ViNodeSpi> futureNode = new FutureBox<ViNodeSpi>();
		private ViNodeSpi node;
		
		private Map<Class<?>, Object> extentions = new HashMap<Class<?>, Object>();
		
		private List<String> labels = new ArrayList<String>();
		private Map<String, Object> userProps = new HashMap<String, Object>();
		
		private boolean terminated = false;
		
		public ManagedNode(String name) {
			this.name = name;
		}
		
		@Override
		public Set<String> labels() {
			return new WriteThroughSet<String>(labels) {

				@Override
				protected boolean onInsert(String label) {
					synchronized(proxy) {
						if (pending) {
							throw new IllegalStateException("Label can only be added before node is initialized");
						}
						else {
							labels.add(label);
						}
						return false;
					}
				}

				@Override
				protected boolean onRemove(String key) {
					throw new UnsupportedOperationException("Label cannot be removed");
				}
			};
		}

		@Override
		public Map<String, Object> userProps() {
			return userProps;
		}

		@Override
		public void shutdown() {
			if (!terminated) {
				terminated = true;
				node.shutdown();
			}
		}

		@Override
		public boolean isConfigured() {
			return pending;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public ViNode getProxy() {
			return proxy;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			synchronized(proxy) {
				if (node == null) {
					LazyMode lmode = method2mode.get(method);
					if (lmode == LazyMode.SMART_DEFERABLE) {
						return createDeferedCall(proxy, method, args);
					}
					else if (lmode == LazyMode.SPI_REQUIRED) {
						ensureSpi();
					}
				}
				Class<?> extc =  method2ext.get(method);
				if (extc == null) {
					// special case, handle internally
					return method.invoke(this, args);
				}
				else {
					return method.invoke(adapt(extc), args);
				}
			}
		}
		
		private Object createDeferedCall(final Object proxy, final Method method, final Object[] args) {
			synchronized(proxy) {
				if (method.getReturnType() == void.class) {
					// one way call
					requestSpi();
					futureNode.addListener(new Box<ViNodeSpi>() {
						@Override
						public void setData(ViNodeSpi data) {
							try {
								invoke(proxy, method, args);
							} catch (Throwable e) {
								// TODO ???
								e.printStackTrace();
							}
						}

						@Override
						public void setError(Throwable e) {
							// TODO ???
							e.printStackTrace();
						}
					});
					return null;
				}
				else if (method.getReturnType().isAssignableFrom(FutureBox.class) 
						&& FutureEx.class.isAssignableFrom(method.getReturnType())) {
					requestSpi();
					final FutureBox<Object> future = new FutureBox<Object>();
					futureNode.addListener(new Box<ViNodeSpi>() {
						
						@SuppressWarnings("unchecked")
						@Override
						public void setData(ViNodeSpi data) {
							try {
								FutureEx<Object> callFuture = (FutureEx<Object>) invoke(proxy, method, args);
								callFuture.addListener(future);
							} catch (Throwable e) {
								future.setError(e);
							}
						}

						@Override
						public void setError(Throwable e) {
							future.setError(e);
						}
					});
					return future;
				}
				else {
					throw new IllegalArgumentException("Cannot defer method call: " + method);
				}
			}
		}
			
		private void ensureSpi() {
			synchronized(proxy) {
				while(true) {
					if (futureNode.isDone()) {
						try {
							futureNode.get();
						} catch (Exception e) {
							if (e instanceof ExecutionException) {
								AnyThrow.throwUncheked(e.getCause());
							}
							else {
								AnyThrow.throwUncheked(e);
							}
							throw new Error("Unrachable");
						}
					}
					else {
						try {
							proxy.wait();
						} catch (InterruptedException e) {
							AnyThrow.throwUncheked(e);
						}
					}					
				}
			}			
		}
			
		private void requestSpi() {
			synchronized(proxy) {
				if (!pending) {
					pending = true;
					taskService.schedule(new InitSpiTask(this, buildConfig()));
				}
			}						
		}
		
		private AttrList buildConfig() {
			AttrList config = new AttrList();
			config.add(AttrBag.NAME, name);
			config.add(AttrBag.TYPE, ViNodeSpi.class.getName());
			
			for(Class<?> extc: extentions.keySet()) {
				ViCloudExtention<?> provider = intf2ext.get(extc);
				provider.processNodeConfig(this, config);
			}
			
			return config;
		}

		@Override
		public ViNodeSpi getCoreNode() {
			synchronized (proxy) {
				return node;
			}
		}
		
		@Override
		public FutureEx<ViNodeSpi> getCoreFuture() {
			synchronized (proxy) {
				return futureNode;
			}
		}

		@Override
		@SuppressWarnings({ "unchecked", "hiding" })
		public <V> V adapt(Class<V> facade) {
			synchronized (proxy) {
				Object ext = extentions.get(facade);
				if (ext == null) {
					ViCloudExtention<?> provider = intf2ext.get(facade);
					ext = provider.wrapSingle(this);
					extentions.put(facade, ext);
				}
				return (V)ext;
			}
		}

		// Stubs to complement ViNode interface
		
		@Override
		public void exec(Runnable task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void exec(VoidCallable task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T exec(Callable<T> task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public FutureEx<Void> submit(Runnable task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public FutureEx<Void> submit(VoidCallable task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> FutureEx<T> submit(Callable<T> task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> List<T> massExec(Callable<? extends T> task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<FutureEx<Void>> massSubmit(Runnable task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<FutureEx<Void>> massSubmit(VoidCallable task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> List<FutureEx<T>> massSubmit(Callable<? extends T> task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> FutureEx<List<T>> vectorSubmit(Callable<? extends T> task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void execute(Runnable task) {
			throw new UnsupportedOperationException();
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
}
