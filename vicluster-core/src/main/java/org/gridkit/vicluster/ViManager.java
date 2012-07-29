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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ViManager implements ViNodeSet {

	private Map<String, ManagedNode> liveNodes = new TreeMap<String, ManagedNode>();
	private Map<String, ManagedNode> deadNodes = new TreeMap<String, ManagedNode>();
	private Map<String, NodeSelector> dynamicSelectors = new LinkedHashMap<String, NodeSelector>();
	
	private ViNodeProvider provider;
	
	public ViManager(ViNodeProvider provider) {
		this.provider = provider;
	}
	
	public ViNodeProvider getProvider() {
		return provider;
	}

	@Override
	public synchronized ViNode node(String namePattern) {
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

	private synchronized void inferConfiguration(ManagedNode node) {
		String name = node.name;
		for(NodeSelector selector: dynamicSelectors.values()) {
			if (selector.match(name)) {
				selector.config.apply(node);
			}
		}		
	}

	private boolean isPattern(String namePattern) {		
		return namePattern.indexOf('*') >= 0 || namePattern.indexOf('?') >= 0;
	}

	@Override
	public synchronized Collection<ViNode> listNodes(String namePattern) {
		Pattern regEx = GlobHelper.translate(namePattern, ".");
		List<ViNode> result = new ArrayList<ViNode>();
		for(ManagedNode vinode: liveNodes.values()) {
			if (regEx.matcher(vinode.name).matches()) {
				result.add(vinode);
			}
		}
		return result;
	}

	protected Collection<ViNode> listNodes(Pattern regEx) {
		List<ViNode> result = new ArrayList<ViNode>();
		for(ManagedNode vinode: liveNodes.values()) {
			if (regEx.matcher(vinode.name).matches()) {
				result.add(vinode);
			}
		}
		return result;
	}

	@Override
	public synchronized void shutdown() {
		// TODO flag terminated state
		ViGroup.group(liveNodes.values()).shutdown();
	}
	
	public synchronized void resetDeadNode() {
		deadNodes.clear();
	}
	
	protected synchronized void markAsDead(ManagedNode node) {
		liveNodes.remove(node.name);
		deadNodes.put(node.name, node);
	}
	
	private class ManagedNode implements ViNode {

		private String name;
		private ViNodeConfig config = new ViNodeConfig();
		private ViNode realNode;
		private boolean terminated;
		
		public ManagedNode(String name) {
			this.name = name;
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
		public void addStartupHook(String name, Runnable hook, boolean override) {
			ensureAlive();
			config.addStartupHook(name, hook, override);
			if (realNode != null) {
				realNode.addStartupHook(name, hook, override);
			}			
		}

		@Override
		public synchronized void addShutdownHook(String name, Runnable hook, boolean override) {
			ensureAlive();
			config.addShutdownHook(name, hook, override);
			if (realNode != null) {
				realNode.addShutdownHook(name, hook, override);
			}			
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
			ensureStarted();
			return realNode.submit(task);
		}

		@Override
		public Future<Void> submit(VoidCallable task) {
			ensureStarted();
			return realNode.submit(task);
		}

		@Override
		public synchronized <T> Future<T> submit(Callable<T> task) {
			ensureStarted();
			return realNode.submit(task);
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
		public synchronized void suspend() {
			ensureStarted();
			realNode.suspend();
		}

		@Override
		public synchronized void resume() {
			ensureStarted();
			realNode.resume();
		}

		@Override
		public synchronized void shutdown() {
			if (!terminated) {
				if (realNode != null) {
					realNode.shutdown();
					realNode = null;
				}
				
				terminated = true;
				ViManager.this.markAsDead(this);
			}
		}

		private synchronized void ensureStarted() {
			if (terminated) {
				throw new IllegalStateException("Node " + name + " is terminated");
			}
			if (realNode == null) {
				realNode = provider.createNode(name, config);
			}
		}
		
		private synchronized void ensureAlive() {
			if (terminated) {
				throw new IllegalStateException("Node " + name + " is terminated");
			}
		}
	}
	
	private class NodeSelector implements ViNode {
		
		@SuppressWarnings("unused")
		private String pattern;
		private Pattern regEx;
		
		private ViNodeConfig config = new ViNodeConfig();

		public NodeSelector(String pattern) {
			this.pattern = pattern;
			this.regEx = GlobHelper.translate(pattern, ".");
		}
		
		public boolean match(String name) {
			return regEx.matcher(name).matches();
		}

		private ViGroup select() {
			return ViGroup.group(listNodes(regEx));
		}
		
		@Override
		public void setProp(String propName, String value) {
			synchronized(ViManager.this) { 
				config.setProp(propName, value);
				select().setProp(propName, value);
			}
		}

		@Override
		public void setProps(Map<String, String> props) {
			synchronized(ViManager.this) { 
				config.setProps(props);
				select().setProps(props);
			}
		}

		@Override
		public void addStartupHook(String name, Runnable hook, boolean override) {
			synchronized(ViManager.this) { 
				config.addStartupHook(name, hook, override);
				select().addStartupHook(name, hook, override);
			}
		}

		@Override
		public void addShutdownHook(String name, Runnable hook, boolean override) {
			synchronized(ViManager.this) { 
				config.addShutdownHook(name, hook, override);
				select().addShutdownHook(name, hook, override);
			}
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
		public void suspend() {
			select().suspend();			
		}

		@Override
		public void resume() {
			select().resume();			
		}

		@Override
		public void shutdown() {
			select().shutdown();
		}
	}
}
