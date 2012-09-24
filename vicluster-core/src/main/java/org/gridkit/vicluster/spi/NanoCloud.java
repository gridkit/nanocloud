package org.gridkit.vicluster.spi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.util.concurrent.TaskService.Task;
import org.gridkit.vicluster.MassExec;
import org.gridkit.vicluster.ViCloud2;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.spi.ViCloudExtention.DeferingMode;
import org.gridkit.vicluster.spi.ViCloudExtention.DynNode;
import org.gridkit.vicluster.spi.ViCloudExtention.GroupCallMode;
import org.gridkit.vicluster.spi.ViCloudExtention.NodeCallProxy;

class NanoCloud<V extends ViNode> implements ViCloud2<V> {

	private final Class<V> facade;
	
	private final CloudContext configContext = new CloudContext();
	
	private final Map<String, ManagedNode> nodes = new TreeMap<String, NanoCloud<V>.ManagedNode>();
	
	private final List<StickyGroupAction> stickyActions  = new ArrayList<StickyGroupAction>();

	private final Map<Class<?>, ViCloudExtention<?>> intf2ext = new LinkedHashMap<Class<?>, ViCloudExtention<?>>();
	private final Map<Method, Class<?>> method2ext = new HashMap<Method, Class<?>>();
	private final Map<Method, ViCloudExtention.DeferingMode> method2mode = new HashMap<Method, ViCloudExtention.DeferingMode>();
	private final Map<Method, ViCloudExtention.GroupCallMode> method2group = new HashMap<Method, ViCloudExtention.GroupCallMode>();
	
	private final TaskService taskService;
	
	private Method methodLabel;
	
	public NanoCloud(Class<V> facade, Collection<ViCloudExtention<?>> extentions, TaskService taskService) {
		this.facade = facade;
		this.taskService = taskService;
		addExtention(new ViExecutorExtetion());
		addExtention(new ViUserPropExtention());
		if (extentions != null) {
			for(ViCloudExtention<?> ext: extentions) {
				addExtention(ext);
			}
		}
		compileMethodMap(facade);
		initBuiltins();
	}
	
	private void initBuiltins() {
		try {
			methodLabel = ViNode.class.getMethod("label", String.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}

	private void addExtention(ViCloudExtention<?> ext) {
		intf2ext.put(ext.getFacadeInterface(), ext);
		for(Class<?> c: ext.getHidenInterfaces()) {
			intf2ext.put(c, ext);
			compileMethodMap(c, ext.getFacadeInterface());
		}
	}

	private void compileMethodMap(Class<?> intf) {
		for(Method m: intf.getMethods()) {
			resolveMethod(m);
		}
		for(Class<?> sup: intf.getInterfaces()) {
			compileMethodMap(sup);
		}		
	}

	private void compileMethodMap(Class<?> intf, Class<?> host) {
		for(Method m: intf.getMethods()) {
			resolveMethod(m, host);
		}
		for(Class<?> sup: intf.getInterfaces()) {
			compileMethodMap(sup, host);
		}		
	}

	private void resolveMethod(Method m) {
		for(Class<?> extif: intf2ext.keySet()) {
			try {
				if (extif.getMethod(m.getName(), m.getParameterTypes()) != null) {
					method2ext.put(m, extif);
					method2mode.put(m, intf2ext.get(extif).deferingModeForMethod(m));
					method2group.put(m, intf2ext.get(extif).groupModeForMethod(m));
					return;
				}
			} catch (SecurityException e) {
				Any.throwUncheked(e);
			} catch (NoSuchMethodException e) {
				continue;
			}
		}
		try {
			if (ViNode.class.getMethod(m.getName(), m.getParameterTypes()) != null) {
				method2ext.put(m, null);
				method2mode.put(m, DeferingMode.NO_SPI_NEEDED);
				if (m.getName().equals("shutdown")) {
					method2group.put(m, GroupCallMode.INSTANT_BROADCAST);
				}
				else if (m.getName().equals("labels")) {
					method2group.put(m, GroupCallMode.BY_IMPLEMENTATION);
				}
				else if (m.getName().equals("label")) {
					method2group.put(m, GroupCallMode.STICKY_BROADCAST);
				}
				else {
					throw new IllegalArgumentException("Unknown method " + m);
				}
				return;
			}
		} catch (SecurityException e) {
			Any.throwUncheked(e);
		} catch (NoSuchMethodException e) {
			// ok
		}
		throw new IllegalArgumentException("Cannot find extension for " + m);
	}

	private void resolveMethod(Method m, Class<?> extif) {
		method2ext.put(m, extif);
		method2mode.put(m, intf2ext.get(extif).deferingModeForMethod(m));
		method2group.put(m, intf2ext.get(extif).groupModeForMethod(m));
		return;
	}
	
	public synchronized void applyConfig(CloudConfigSet configSet) {
		((RuleSet)configSet).apply(configContext);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public synchronized V byName(String pattern) {
		if (isPattern(pattern)) {
			NodeMatcher matcher = new RegExNodeMatcher(pattern);
			return createGroupProxy(matcher);
		}
		ManagedNode mn = ensureNode(pattern);
		return (V)mn.getProxy();
	}

	@Override
	public synchronized V byLabel(String label) {
		return createGroupProxy(new HasLabel(label));
	}

	@Override
	public List<V> listByName(String pattern) {
		RegExNodeMatcher matcher = new RegExNodeMatcher(pattern);
		return selectProxies(matcher);
	}

	@Override
	public List<V> listByLabel(String label) {
		HasLabel matcher = new HasLabel(label);
		return selectProxies(matcher);		
	}

	@SuppressWarnings("unchecked")
	private synchronized List<V> selectProxies(NodeMatcher matcher) {
		List<ManagedNode> nodes = selectNodes(matcher);
		List<V> result = new ArrayList<V>();
		for(ManagedNode node: nodes) {
			result.add((V)node.getProxy());
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private V createGroupProxy(NodeMatcher matcher) {
		ManagedGroup g = new ManagedGroup(matcher);
		Object proxy = Proxy.newProxyInstance(facade.getClassLoader(), new Class[]{facade}, g);
		return (V)proxy;
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
			applyStickyGroupActions(mn);
		}
		return nodes.get(name);
	}
	
	private List<ManagedNode> selectNodes(NodeMatcher matcher) {
		List<ManagedNode> result = new ArrayList<NanoCloud<V>.ManagedNode>();
		for(ManagedNode node: nodes.values()) {
			if (!node.isTerminated() && matcher.match(node)) {
				result.add(node);
			}
		}
		return result;
	}
	
	@Override
	public void shutdown() {
	}
	
	private void createStickyAction(NodeMatcher matcher, Method method, Object[] args) {
		StickyGroupAction action  = new StickyGroupAction();
		action.matcher = matcher;
		action.method = method;
		action.arguments = args;
		
		stickyActions.add(action);
		
		for(ManagedNode node: nodes.values()) {
			if (!node.isTerminated()) {
				applyStickyGroupActions(node);
			}
		}		
	}
	
	private void applyStickyGroupActions(ManagedNode node) {
		for(StickyGroupAction action: stickyActions) {
			if (!node.appliedActions.contains(action) && action.matcher.match(node)) {
				node.appliedActions.add(action);
				try {
					node.dispatch(action.method, action.arguments);
				} catch (Throwable e) {
					if (e instanceof InvocationTargetException) {
						e = e.getCause();
					}
					Any.throwUncheked(e);
				}
			}
		}
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
				synchronized(NanoCloud.this) {
					System.out.println("Node initialization failed: " + node.name);
					node.futureNode.setError(e);
					NanoCloud.this.notifyAll();
				}
				return;
			}
			synchronized(NanoCloud.this) {
				System.out.println("Node initialized: " + node.name + " -> " + coreNode);
				node.node = coreNode;
				node.futureNode.setData(coreNode);
				NanoCloud.this.notifyAll();
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

	private class ManagedNode extends ViNodeStub implements ViNode, DynNode, InvocationHandler {

		private final String name;
		
		private ViNode proxy;
		
		private boolean pending;
		private FutureBox<ViNodeSpi> futureNode = new FutureBox<ViNodeSpi>();
		private ViNodeSpi node;
		
		private Map<Class<?>, Object> extentions = new HashMap<Class<?>, Object>();
		
		private List<String> labels = new ArrayList<String>();
		private Set<StickyGroupAction> appliedActions = new HashSet<StickyGroupAction>();
		
		private boolean terminated = false;
		
		public ManagedNode(String name) {
			this.name = name;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String[] getLabels() {
			return labels.toArray(new String[labels.size()]);
		}

		@Override
		public Set<String> labels() {
			return new WriteThroughSet<String>(labels) {

				@Override
				protected boolean onInsert(String label) {
					synchronized(NanoCloud.this) {
						if (pending) {
							throw new IllegalStateException("Label can only be added before node is initialized");
						}
						else {
							label(label);
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
		public void label(String label) {
			if (pending) {
				throw new IllegalStateException("Label can only be added before node is initialized");
			}
			else if (!labels.contains(label)) {
				labels.add(label);
				applyStickyGroupActions(this);
			}
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
			if (method.getDeclaringClass().equals(Object.class)) {
				return method.invoke(this, args);
			}
			return dispatch(method, args);
		}

		@Override
		public Object dispatch(Method method, Object... args) throws Throwable {
			synchronized(NanoCloud.this) {
				if (node == null) {
					DeferingMode lmode = method2mode.get(method);
					if (lmode == DeferingMode.SMART_DEFERABLE) {
						return createDeferedCall(method, args);
					}
					else if (lmode == DeferingMode.SPI_REQUIRED) {
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
		
		private Object createDeferedCall(final Method method, final Object[] args) {
			synchronized(NanoCloud.this) {
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
								FutureEx<Object> callFuture = (FutureEx<Object>) dispatch(method, args);
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
			synchronized(NanoCloud.this) {
				requestSpi();
				while(true) {
					if (futureNode.isDone()) {
						try {
							futureNode.get();
							return;
						} catch (Exception e) {
							if (e instanceof ExecutionException) {
								Any.throwUncheked(e.getCause());
							}
							else {
								Any.throwUncheked(e);
							}
							throw new Error("Unrachable");
						}
					}
					else {
						try {
							// we need to wait on NanoCloud object to temporarely
							// release semaphore and thus let init task do its work
							NanoCloud.this.wait();
						} catch (InterruptedException e) {
							Any.throwUncheked(e);
						}
					}					
				}
			}			
		}
			
		private void requestSpi() {
			synchronized(NanoCloud.this) {
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
			for(String label: labels) {
				config.add(AttrBag.LABEL, label);
			}
			
			for(Class<?> extc: extentions.keySet()) {
				ViCloudExtention<?> provider = intf2ext.get(extc);
				provider.processNodeConfig(this, config);
			}
			
			return config;
		}

		@Override
		public ViNodeSpi getCoreNode() {
			synchronized (NanoCloud.this) {
				return node;
			}
		}
		
		@Override
		public FutureEx<ViNodeSpi> getCoreFuture() {
			synchronized (NanoCloud.this) {
				return futureNode;
			}
		}

		@Override
		@SuppressWarnings({ "unchecked", "hiding" })
		public <V> V adapt(Class<V> facade) {
			synchronized (NanoCloud.this) {
				Object ext = extentions.get(facade);
				if (ext == null) {
					ViCloudExtention<?> provider = intf2ext.get(facade);
					ext = provider.wrapSingle(this);
					extentions.put(facade, ext);
				}
				return (V)ext;
			}
		}
		
		public String toString() {
			return name;
		}

	}

	private class ManagedGroup extends ViNodeStub implements ViNode, NodeCallProxy, InvocationHandler {

		private NodeMatcher matcher;
		
		
		public ManagedGroup(NodeMatcher matcher) {
			this.matcher = matcher;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return dispatch(method, args);
		}

		@Override
		public Object dispatch(Method method, Object... args) throws Throwable {
			if (method.getDeclaringClass().equals(Object.class)) {
				return method.invoke(this, args);
			}
			synchronized(NanoCloud.this) {
				List<ManagedNode> nodes = selectNodes(matcher);

				// parallel invoke
				GroupCallMode mode = method2group.get(method);

				// TODO this optimization doesn't seems to make sense
//				if (nodes.isEmpty()) {
//					throw new IllegalArgumentException("Node selection is empty");
//				}
//				if (nodes.size() == 1 && mode != GroupCallMode.STICKY_BROADCAST) {
//					return nodes.get(0).dispatch(method, args);
//				}
				
				switch(mode) {
				case UNSUPPORTED:
					throw new UnsupportedOperationException("Parallel invocation is unsupported for " + method);
				case INSTANT_BROADCAST:
					broadcast(nodes, method, args);
					return null;
				case BY_IMPLEMENTATION:
					return wrapperInvoke(nodes, method, args);
				case STICKY_BROADCAST:
					createStickyAction(matcher, method, args);
					return null;
				}
				throw new Error("Unreachable");
			}
		}

		private void broadcast(List<ManagedNode> nodes, Method method, Object[] args) throws Throwable {
			for(ManagedNode node: nodes) {
				node.dispatch(method, args);
			}
		}
		
		private Object wrapperInvoke(List<ManagedNode> nodes, Method method, Object[] args) throws Throwable {
			DeferingMode mode = method2mode.get(method);
			if (mode == DeferingMode.SPI_REQUIRED) {
				ensureSpi(nodes);
			}
			if (mode == DeferingMode.SMART_DEFERABLE) {
				if (!isSpiReady(nodes)) {
					return createDeferedCall(nodes, method, args);
				}
			}
			
			return invokeOnWrapper(nodes, method, args);
		}

		private void ensureSpi(List<ManagedNode> nodes) {
			synchronized(NanoCloud.this) {
				FutureEx<List<ViNodeSpi>> initFuture = MassExec.vectorFuture(requestSpi(nodes));
				while(true) {
					if (initFuture.isDone()) {
						try {
							initFuture.get();
							return;
						} catch (Exception e) {
							if (e instanceof ExecutionException) {
								Any.throwUncheked(e.getCause());
							}
							else {
								Any.throwUncheked(e);
							}
							throw new Error("Unrachable");
						}
					}
					else {
						try {
							// we need to wait on NanoCloud object to temporarely
							// release semaphore and thus let init task do its work
							NanoCloud.this.wait();
						} catch (InterruptedException e) {
							Any.throwUncheked(e);
						}
					}					
				}
			}			
		}

		private List<FutureEx<ViNodeSpi>> requestSpi(List<ManagedNode> nodes) {
			List<FutureEx<ViNodeSpi>> futures = new ArrayList<FutureEx<ViNodeSpi>>();
			for(ManagedNode node: nodes) {
				node.requestSpi();
				futures.add(node.getCoreFuture());
			}
			return futures;
		}

		private boolean isSpiReady(List<ManagedNode> nodes) {
			for(ManagedNode node: nodes) {
				if (node.node == null) {
					return false;
				}
			}
			return true;
		}

		private Object invokeOnWrapper(List<ManagedNode> nodes, final Method method, final Object[] args) throws IllegalAccessException, Throwable {
			Class<?> extif = method2ext.get(method);
			ViCloudExtention<?> ext = intf2ext.get(extif);
			Object wrapper;
			if (ext != null) {
				wrapper = ext.wrapMultiple(this, nodes.toArray(new DynNode[nodes.size()]));
			}
			else {
				wrapper = this;
			}
			try {
				return method.invoke(wrapper, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}

		private Object createDeferedCall(final List<ManagedNode> nodes, final Method method, final Object[] args) {
			synchronized(NanoCloud.this) {
				if (method.getReturnType() == void.class) {
					// one way call
					FutureEx<List<ViNodeSpi>> spiFuture = MassExec.vectorFuture(requestSpi(nodes)); 
					spiFuture.addListener(new Box<List<ViNodeSpi>>() {
						@Override
						public void setData(List<ViNodeSpi> data) {
							try {
								invokeOnWrapper(nodes, method, args);
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
					FutureEx<List<ViNodeSpi>> spiFuture = MassExec.vectorFuture(requestSpi(nodes)); 
					final FutureBox<Object> future = new FutureBox<Object>();
					spiFuture.addListener(new Box<List<ViNodeSpi>>() {
						
						@SuppressWarnings("unchecked")
						@Override
						public void setData(List<ViNodeSpi> data) {
							try {
								FutureEx<Object> callFuture = (FutureEx<Object>) invokeOnWrapper(nodes, method, args);
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
		
		@Override
		public Set<String> labels() {
			return new WriteThroughSet<String>(new HashSet<String>()) {

				@Override
				protected boolean onInsert(String key) {
					try {
						dispatch(methodLabel, new Object[]{key});
						return false;
					} catch (Throwable e) {
						if (e instanceof InvocationTargetException) {
							e = e.getCause();
						}
						Any.throwUncheked(e);
						throw new Error("Unreachable");
					}
				}

				@Override
				protected boolean onRemove(String key) {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public void label(String label) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void shutdown() {
			throw new UnsupportedOperationException();
		}

		public String toString() {
			return "group{"  + matcher + "}";
		}		
	}
	
	private interface NodeMatcher {
		
		public boolean match(DynNode node);
		
	}
	
	private static class RegExNodeMatcher implements NodeMatcher {
		
		private final Pattern pattern;

		public RegExNodeMatcher(String glob) {			
			this(GlobHelper.translate(glob, "."));
		}

		public RegExNodeMatcher(Pattern pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean match(DynNode node) {
			String name = node.getName();			
			return pattern.matcher(name).matches();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((pattern.pattern() == null) ? 0 : pattern.pattern().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RegExNodeMatcher other = (RegExNodeMatcher) obj;
			if (pattern == null) {
				if (other.pattern != null)
					return false;
			} else if (!pattern.pattern().equals(other.pattern.pattern()))
				return false;
			return true;
		}
		
		public String toString() {
			return pattern.pattern();
		}
	}
	
	private static class HasLabel implements NodeMatcher {

		private final String label;
		
		public HasLabel(String label) {
			this.label = label;
		}

		@Override
		public boolean match(DynNode node) {
			for(String l: node.getLabels()) {
				if (label.equals(l)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HasLabel other = (HasLabel) obj;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "@" + label;
		}
	}
	
	@SuppressWarnings("unused")
	private static class AndMatcher implements NodeMatcher {
	
		private NodeMatcher[] matchers;

		public AndMatcher(NodeMatcher... matchers) {
			this.matchers = matchers;
		}

		@Override
		public boolean match(DynNode node) {
			for(NodeMatcher m: matchers) {
				if (!m.match(node)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(matchers);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AndMatcher other = (AndMatcher) obj;
			if (!Arrays.equals(matchers, other.matchers))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "AND " + Arrays.toString(matchers); 
		}
	}

	@SuppressWarnings("unused")
	private static class OrMatcher implements NodeMatcher {
		
		private NodeMatcher[] matchers;

		public OrMatcher(NodeMatcher... matchers) {
			this.matchers = matchers;
		}

		@Override
		public boolean match(DynNode node) {
			for(NodeMatcher m: matchers) {
				if (m.match(node)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(matchers);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OrMatcher other = (OrMatcher) obj;
			if (!Arrays.equals(matchers, other.matchers))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "OR " + Arrays.toString(matchers); 
		}
	}
	
	private static class StickyGroupAction {
		
		NodeMatcher matcher;
		Method method;
		Object[] arguments;		
				
		public String toString() {
			return matcher + " -> " + method.getName() + " " + Arrays.toString(arguments);
		}
	}	
}
