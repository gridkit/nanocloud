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
package org.gridkit.vicluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.ExceptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ViManager implements ViNodeSet {

	private final static Logger LOGGER = LoggerFactory.getLogger(ViNodeSet.class);
	
	private Map<String, ManagedNode> liveNodes = new TreeMap<String, ManagedNode>();
	private Map<String, ManagedNode> deadNodes = new TreeMap<String, ManagedNode>();
	private Map<String, NodeSelector> dynamicSelectors = new LinkedHashMap<String, NodeSelector>();
	
	private ViNodeProvider provider;
	private ExecutorService asyncInitThreads;
	private boolean terminated = false;
	
	private long ruleCounter = 0;
	
	public ViManager(ViNodeProvider provider) {
		this(provider, 32);
	}

	public ViManager(ViNodeProvider provider, int deferedTaskLimit) {
		this.provider = provider;
		if (deferedTaskLimit == 0) {
			asyncInitThreads = Executors.newSingleThreadExecutor();
		}
		else {
			asyncInitThreads = new ThreadPoolExecutor(deferedTaskLimit, deferedTaskLimit, 100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1024), new ThreadFactory() {
				
				int counter = 1;
				
				@Override
				public synchronized Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("ViManager-worker-" + (counter++));
					t.setDaemon(true);
					return t;
				}
			});
		}
	}
	
	public ViNodeProvider getProvider() {
		return provider;
	}

	private void ensureAlive() {
		if (terminated) {
			throw new IllegalStateException("Cloud has been terminated");
		}
	}

	@Override
	public synchronized ViNode node(String namePattern) {
		ensureAlive();
		if (liveNodes.containsKey(namePattern)) {
			return liveNodes.get(namePattern);
		}
		else if (deadNodes.containsKey(namePattern)) {
			return deadNodes.get(namePattern);
		}
		else if (dynamicSelectors.containsKey(namePattern)){
			return dynamicSelectors.get(namePattern);
		}
		else if (isPattern(namePattern)) {
			NodeSelector selector = new NodeSelector(namePattern);
			dynamicSelectors.put(namePattern, selector);
			return selector;
		}
		else {
			String name = namePattern;
			ManagedNode node = new ManagedNode(name);
			inferConfiguration(node);
			liveNodes.put(name, node);
			return node;
		}
	}

	public ViNode nodes(String... patterns) {
		ensureAlive();
		Set<ViNode> nodes = new LinkedHashSet<ViNode>();
		for(String pattern: patterns) {
			nodes.add(node(pattern));
		}
		return ViGroup.group(nodes);
	}
	
	private synchronized void inferConfiguration(ManagedNode node) {
		List<Rule> rules = new ArrayList<ViManager.Rule>();
		for(NodeSelector selector: dynamicSelectors.values()) {
			if (selector.match(node)) {
				rules.addAll(selector.rules);
			}
		}		
		Collections.sort(rules);
		for(Rule rule: rules) {
			rule.getConfig().apply(node);
		}
	}

	private boolean isPattern(String namePattern) {		
		return namePattern.indexOf('*') >= 0 || namePattern.indexOf('?') >= 0;
	}

	@Override
	public synchronized Collection<ViNode> listNodes(String namePattern) {
		ensureAlive();
		Set<ViNode> result = new LinkedHashSet<ViNode>();
		Pattern regEx = GlobHelper.translate(namePattern, ".");
		for(ManagedNode vinode: liveNodes.values()) {
			if (match(regEx, vinode)) {
				result.add(vinode);
			}
		}
		return result;
	}

	protected Collection<ViNode> listNodes(Pattern regEx) {
		if (liveNodes.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			List<ViNode> result = new ArrayList<ViNode>();
			for(ManagedNode vinode: liveNodes.values()) {
				if (match(regEx, vinode) ) {
					result.add(vinode);
				}
			}
			return result;
		}
	}

	private static boolean match(Pattern regEx, ManagedNode vinode) {
		return regEx.matcher(vinode.name).matches()
				|| regEx.matcher("." + vinode.name).matches()
				|| regEx.matcher(vinode.name + ".").matches()
				|| regEx.matcher("." + vinode.name + ".").matches();
	}

	@Override
	public void shutdown() {
		// TODO concurrency design is rather poor for this class
		List<Future<?>> epitaphs;
		synchronized(this) {
			if (terminated == true) {
				return;
			}
			terminated = true;
			epitaphs = new ArrayList<Future<?>>();
			for(final ManagedNode node: liveNodes.values()) {
				epitaphs.add(asyncInitThreads.submit(new Runnable() {
					@Override
					public void run() {
						try {
							node.shutdown();
						}
						catch(Exception e) {
							LOGGER.warn("Exception on shutdown for '" + node.name + "'", e);
						}
					}
				}));
			}
		}
		for(Future<?> e: epitaphs) {
			try {
				e.get();
			} catch (InterruptedException ee) {
				break;
			} catch (ExecutionException ee) {
				LOGGER.warn("Exception on shutdown", ee.getCause());
			}
		}
		// there could be a race between initialization and shutdown here
		asyncInitThreads.shutdown();
		try {
			asyncInitThreads.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.warn("ViManager shutdown: Defered task threads are still active");
		}		
	}
	
	public synchronized void resetDeadNode() {
		ensureAlive();
		deadNodes.clear();
	}
	
	protected synchronized void markAsDead(ManagedNode node) {
		liveNodes.remove(node.name);
		deadNodes.put(node.name, node);
	}
	
	protected synchronized Rule newRule(NodeSelector selector) {
		Rule rule = new Rule(ruleCounter++, selector);
		return rule;
	}
	
	static String transform(String pattern, String name) {
		int n = pattern.indexOf('!');
		if (n < 0) {
			throw new IllegalArgumentException("Invalid host extractor [" + pattern + "]");
		}
		String format = pattern.substring(1, n);
		Matcher m = Pattern.compile(pattern.substring(n + 1)).matcher(name);
		if (!m.matches()) {
			throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
		}
		else {
			Object[] groups = new Object[m.groupCount()];
			for(int i = 0; i != groups.length; ++i) {
				groups[i] = m.group(i + 1);
				try {
					groups[i] = new Long((String)groups[i]);
				}
				catch(NumberFormatException e) {
					// ignore
				}				
			}
			try {
				return String.format(format, groups);
			}
			catch(IllegalArgumentException e) {
				throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
			}
		}
	}
	
	private class ManagedNode implements ViNode {

		private String name;
		private ViNodeConfig config = new ViNodeConfig();
		private ViExecutor nodeExecutor;
		private ViNode realNode;
		private FutureTask<Void> initBarrier = new FutureTask<Void>(new InitTask(), null);
		private boolean terminated;
		
		public ManagedNode(String name) {
			this.name = name;
		}

		@Override
		public <X> X x(ViNodeExtender<X> extention) {
		    return extention.wrap(this);
		}
		
		@Override
		public <X> X x(ViConfExtender<X> extention) {
			return extention.wrap(this);
		}
		
		@Override
		public void setProp(String propName, String value) {
			ensureAlive();
			config.setProp(propName, value);
			if (realNode != null) {
				realNode.setProp(propName, value);
			}
		}

		@Override
		public void setProps(Map<String, String> props) {
			ensureAlive();
			config.setProps(props);
			if (realNode != null) {
				realNode.setProps(props);
			}
		}

		@Override
		public void setConfigElement(String key, Object value) {
			ensureAlive();
			this.config.setConfigElement(key, value);
			if (realNode != null) {
				realNode.setConfigElement(key, value);
			}
		}

		@Override
		public void setConfigElements(Map<String, Object> config) {
			ensureAlive();
			this.config.setConfigElements(config);
			if (realNode != null) {
				realNode.setConfigElements(config);
			}
		}

		@Override
		public void touch() {
			exec(new Touch());
		}

		@Override
		public void exec(Runnable task) {
			MassExec.submitAndWait(this, task);
		}

		@Override
		public void exec(VoidCallable task) {
			MassExec.submitAndWait(this, task);
		}

		@Override
		public <T> T exec(Callable<T> task) {
			return MassExec.submitAndWait(this, task);
		}

		@Override
		public Future<Void> submit(Runnable task) {
			ensureExecutor();
			return nodeExecutor.submit(task);
		}

		@Override
		public Future<Void> submit(VoidCallable task) {
			ensureExecutor();
			return nodeExecutor.submit(task);
		}

		@Override
		public synchronized <T> Future<T> submit(Callable<T> task) {
			ensureExecutor();
			return nodeExecutor.submit(task);
		}

		@Override
		public <T> List<T> massExec(Callable<? extends T> task) {
			return MassExec.singleNodeMassExec(this, task);
		}

		@Override
		public List<Future<Void>> massSubmit(Runnable task) {
			return MassExec.singleNodeMassSubmit(this, task);
		}

		@Override
		public List<Future<Void>> massSubmit(VoidCallable task) {
			return MassExec.singleNodeMassSubmit(this, task);
		}

		@Override
		public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
			return MassExec.singleNodeMassSubmit(this, task);
		}

		@Override
		public synchronized String getProp(String propName) {
			if (realNode != null) {
				return  realNode.getProp(propName);
			}
			else {
				return config.getProp(propName);
			}
		}

		@Override
		public synchronized void shutdown() {
			if (!terminated) {
				if (realNode != null) {
					realNode.shutdown();
					realNode = null;
				}
				if (nodeExecutor != null) {
					nodeExecutor = null;
				}
				
				terminated = true;
				ViManager.this.markAsDead(this);
			}
		}

		@Override
		public synchronized void kill() {
			if (!terminated) {
				if (realNode != null) {
					realNode.kill();
					realNode = null;
				}
				if (nodeExecutor != null) {
					nodeExecutor = null;
				}
				
				terminated = true;
				ViManager.this.markAsDead(this);
			}
		}

		private synchronized void ensureExecutor() {
			if (terminated) {
				throw new IllegalStateException("ViNode[" + name + "] is terminated");
			}
			if (nodeExecutor == null) {
				nodeExecutor = new DeferedNodeExecutor(name, initBarrier, asyncInitThreads, this);
				LOGGER.debug("ViNode[" + name + "] instantiating");
				
				asyncInitThreads.execute(initBarrier);
			}
		}
		
		private synchronized void ensureAlive() {
			if (terminated) {
				throw new IllegalStateException("ViNode[" + name + "] is terminated");
			}
		}
		
		public String toString() {
			return name;
		}

		private ViNode createNode() {
			if (ViProps.NODE_TYPE_ALIAS.equals(config.getProp(ViProps.NODE_TYPE))) {
				String host = config.getProp(ViProps.HOST);
				if (host == null) {
					throw new IllegalArgumentException("No host specified for node '" + name + "'");
				}
				if (host.startsWith("~")) {
					host = transform(host, name);
				}
				ViNode hostnode = node(host);
				return new ProxyViNode(name, hostnode);				
			}
			else {
				ViNode realNode = provider.createNode(name, config);
				return realNode;
			}
		}
		
		private final class InitTask implements Runnable {
			@Override
			public void run() {
				String tname = swapThreadName("ViNode[" + name + "] init");
				try {
					try {
						ViNode realNode = createNode();
						synchronized(ManagedNode.this) {
							if (terminated) {
								realNode.shutdown();
							}
							else {
								ManagedNode.this.realNode = realNode;
								nodeExecutor = realNode;
								LOGGER.debug("ViNode[" + name + "] instantiated");
							}
						}
					}
					catch(RuntimeException e) {
						LOGGER.error("ViNode[" + name + "] initialization has failed", e);
						throw e;
					}
					//TODO handle exception
				}
				finally {
					swapThreadName(tname);
				}
			}
		}
	}
	
	private static class DeferedNodeExecutor implements ViExecutor {

		private String name;
		private Future<Void> barrier;
		private ExecutorService executor;
		private ViExecutor target;

		public DeferedNodeExecutor(String name, Future<Void> barrier, ExecutorService executor, ViExecutor target) {
			this.name = name;
			this.barrier = barrier;
			this.executor = new DoNotForgetStackExecutor(executor);
			this.target = target;
		}

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
		public Future<Void> submit(final Runnable task) {
			return executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					String tname = swapThreadName("ViNode[" + name + "] defered submission " + task.toString());
					try {
						try {
							barrier.get();
						}
						catch(ExecutionException e) {
							if (e.getCause() instanceof Exception) {
								throw ((Exception)e.getCause());
							}
							else {
								throw e;
							}
						}
						target.exec(task);
						return null;
					}
					finally {
						swapThreadName(tname);
					}
				}
			});
		}

		@Override
		public Future<Void> submit(final VoidCallable task) {
			return executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					String tname = swapThreadName("ViNode[" + name + "] defered submission " + task.toString());
					try {
						barrier.get();
						target.exec(task);
						return null;
					}
					finally {
						swapThreadName(tname);
					}
				}
			});
		}

		@Override
		public <T> Future<T> submit(final Callable<T> task) {
			return executor.submit(new Callable<T>() {
				@Override
				public T call() throws Exception {
					String tname = swapThreadName("ViNode[" + name + "] defered submission " + task.toString());
					try {
						barrier.get();
						return target.exec(task);
					}
					finally {
						swapThreadName(tname);
					}
				}
			});
		}

		@Override
		public <T> List<T> massExec(Callable<? extends T> task) {
			return MassExec.singleNodeMassExec(this, task);
		}

		@Override
		public List<Future<Void>> massSubmit(Runnable task) {
			return MassExec.singleNodeMassSubmit(this, task);
		}

		@Override
		public List<Future<Void>> massSubmit(VoidCallable task) {
			return MassExec.singleNodeMassSubmit(this, task);
		}

		@Override
		public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
			return MassExec.singleNodeMassSubmit(this, task);
		}
	}

    private static class DoNotForgetStackExecutor extends AbstractExecutorService {
        private final ExecutorService delegate;

        private DoNotForgetStackExecutor(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, T value) {
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            return super.newTaskFor(new Runnable() {
                @Override
                public void run() {
                    try{
                      runnable.run();
                    }catch (Throwable t){
                        ExceptionHelper.throwUnchecked(
                                ExceptionHelper.addStackElementsAtBottom(
                                        t,
                                        new StackTraceElement("-----submit", "submit", null, -1),
                                        stackTrace)
                        );
                    }
                }
            }, value);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            return super.newTaskFor(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    try{
                        return callable.call();
                    }catch (Throwable t){
                        ExceptionHelper.throwUnchecked(
                                ExceptionHelper.addStackElementsAtBottom(
                                        t,
                                        new StackTraceElement("-----submit", "submit", null, -1),
                                        stackTrace)
                        );
                        throw new IllegalStateException("should never reach here");
                    }
                }
            });
        }
    }
	
	private static class Rule implements Comparable<Rule> {
		
		private final long ruleNo;
		private final NodeSelector selector;
		private final ViNodeConfig config = new ViNodeConfig();
		
		public Rule(long ruleNo, NodeSelector selector) {
			this.ruleNo = ruleNo;
			this.selector = selector;
		}
		
		public ViNodeConfig getConfig() {
			return config;
		}

		@Override
		public int compareTo(Rule o) {
			return Long.signum(ruleNo - o.ruleNo);
		}

		@Override
		public String toString() {
			return "[" + ruleNo + "] " + selector.pattern + " -> " + config.toString();
		}
	}
	
	private class NodeSelector implements ViNode {
		
		private String pattern;
		private Pattern regEx;
		
		private List<Rule> rules = new ArrayList<Rule>();
		
		public NodeSelector(String pattern) {
			this.pattern = pattern;
			this.regEx = GlobHelper.translate(pattern, ".");
		}
		
		@Override
		public <X> X x(ViNodeExtender<X> extention) {
		    return extention.wrap(this);
		}
		
		@Override
		public <X> X x(ViConfExtender<X> extention) {
			return extention.wrap(this);
		}
		
		public boolean match(ManagedNode node) {
			return ViManager.match(regEx, node);
		}

		private ViGroup select() {
			return ViGroup.group(listNodes(regEx));
		}
		
		private ViNodeConfig rule() {
			Rule rule = newRule(this);
			rules.add(rule);
			return rule.getConfig();
		}
		
		@Override
		public void setProp(String propName, String value) {
			synchronized(ViManager.this) { 
				rule().setProp(propName, value);
				select().setProp(propName, value);
			}
		}

		@Override
		public void setProps(Map<String, String> props) {
			synchronized(ViManager.this) {
				rule().setProps(props);
				select().setProps(props);
			}
		}

		@Override
		public void setConfigElement(String key, Object value) {
			synchronized(ViManager.this) {
				rule().setConfigElement(key, value);
				select().setConfigElement(key, value);
			}
		}

		@Override
		public void setConfigElements(Map<String, Object> config) {
			synchronized(ViManager.this) {
				rule().setConfigElements(config);
				select().setConfigElements(config);
			}
		}

		@Override
		public void touch() {
			select().touch();
		}

		@Override
		public void exec(Runnable task) {
			select().exec(task);
		}

		@Override
		public void exec(VoidCallable task) {
			select().exec(task);
		}

		@Override
		public <T> T exec(Callable<T> task) {
			return select().exec(task);
		}

		@Override
		public Future<Void> submit(Runnable task) {
			return select().submit(task);
		}

		@Override
		public Future<Void> submit(VoidCallable task) {
			return select().submit(task);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return select().submit(task);
		}

		@Override
		public <T> List<T> massExec(Callable<? extends T> task) {
			return select().massExec(task);
		}

		@Override
		public List<Future<Void>> massSubmit(Runnable task) {
			return select().massSubmit(task);
		}

		@Override
		public List<Future<Void>> massSubmit(VoidCallable task) {
			return select().massSubmit(task);
		}

		@Override
		public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
			return select().massSubmit(task);
		}

		@Override
		public String getProp(String propName) {
			throw new UnsupportedOperationException("Cannot call on group of nodes");
		}

		@Override
		public void kill() {
			select().kill();
		}

		@Override
		public void shutdown() {
			select().shutdown();
		}
		
		@Override
		public String toString() {
			return pattern;
		}
	}
	
	private static String swapThreadName(String newName) {
		Thread currentThread = Thread.currentThread();
		String name = currentThread.getName();
		currentThread.setName(newName);
		return name;
	}
	
	
	private static class ProxyViNode implements ViNode {
		
		private final String name;
		private final ViNode node;
		
		public ProxyViNode(String name, ViNode node) {
			this.name = name;
			this.node = node;
		}
		
		@Override
		public <X> X x(ViNodeExtender<X> extention) {
		    return extention.wrap(this);
		}
		
		@Override
		public <X> X x(ViConfExtender<X> extention) {
			return extention.wrap(this);
		}
		
		@Override
		public void touch() {
			node.touch();
		}

		@Override
		public void exec(Runnable task) {
			node.exec(task);			
		}
		
		@Override
		public void exec(VoidCallable task) {
			node.exec(task);			
		}
		
		@Override
		public <T> T exec(Callable<T> task) {
			return node.exec(task);
		}
		
		@Override
		public Future<Void> submit(Runnable task) {
			return node.submit(task);
		}
		
		@Override
		public Future<Void> submit(VoidCallable task) {
			return node.submit(task);
		}
		
		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return node.submit(task);
		}
		
		@Override
		public <T> List<T> massExec(Callable<? extends T> task) {
			return node.massExec(task);
		}
		
		@Override
		public List<Future<Void>> massSubmit(Runnable task) {
			return node.massSubmit(task);
		}
		
		@Override
		public List<Future<Void>> massSubmit(VoidCallable task) {
			return node.massSubmit(task);
		}
		
		@Override
		public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
			return node.massSubmit(task);
		}
		
		@Override
		public void setProp(String propName, String value) {
			node.setProp(propName, value);			
		}
		
		@Override
		public void setProps(Map<String, String> props) {
			node.setProps(props);			
		}


		public void setConfigElement(String key, Object value) {
			node.setConfigElement(key, value);
		}

		public void setConfigElements(Map<String, Object> config) {
			node.setConfigElements(config);
		}

		@Override
		public String getProp(String propName) {
			return node.getProp(propName);
		}
		
		@Override
		public void kill() {
			// do nothing
		}
		
		@Override
		public void shutdown() {
			// do nothing
		}
		
		public String toString() {
			return name;
		}
	}	
	
	private static class Touch implements Runnable, Serializable {

		private static final long serialVersionUID = 20121116L;

		@Override
		public void run() {
			// do nothing
		}		
	}
}
