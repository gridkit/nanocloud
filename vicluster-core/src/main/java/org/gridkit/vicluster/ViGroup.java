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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ViGroup implements ViNode {

	public static ViGroup group(ViNode... hosts) {
		ViGroup group = new ViGroup();
		for(ViNode host: hosts) {
			group.addNode(host);
		}
		return group;
	}

	public static ViGroup group(Collection<? extends ViNode> hosts) {
		ViGroup group = new ViGroup();
		for(ViNode host: hosts) {
			group.addNode(host);
		}
		return group;
	}
	
	private ViNodeConfig config = new ViNodeConfig();
	private List<ViNode> hosts = new ArrayList<ViNode>();
	private boolean shutdown = false;
	
	private void checkActive() {
		if (shutdown) {
			throw new IllegalStateException("Group is shutdown");
		}
	}

	private void checkExecutable() {
		checkActive();
		if (hosts.isEmpty()) {
			throw new IllegalStateException("No hosts in this group");
		}
	}
	
	public synchronized void addNode(ViNode host) {
		if (host == null) {
			throw new NullPointerException("null ViNode reference");
		}
		checkActive();
		hosts.add(host);
		config.apply(host);
	}

	@Override
	public <X> X x(ViExtender<X> extention) {
		return extention.wrap(this);
	}

	@Override
	public synchronized void setProp(String propName, String value) {
		checkActive();
		config.setProp(propName, value);
		for(ViNode vh: hosts) {
			vh.setProp(propName, value);
		}
	}
	
	@Override
	public synchronized void setProps(Map<String, String> props) {
		checkActive();
		config.setProps(props);
		for(ViNode vh: hosts) {
			vh.setProps(props);
		}
	}
	
	@Override
	public String getProp(String propName) {
		throw new UnsupportedOperationException("Unsupported for group");
	}

	@Override
	public void setConfigElement(String key, Object value) {
		checkActive();
		config.setConfigElement(key, value);
		for(ViNode vh: hosts) {
			vh.setConfigElement(key, value);
		}
	}

	@Override
	public void setConfigElements(Map<String, Object> config) {
		checkActive();
		this.config.setConfigElements(config);
		for(ViNode vh: hosts) {
			vh.setConfigElements(config);
		}
	}

	
	@Override
	@SuppressWarnings("deprecation")
	public synchronized void addStartupHook(String name, Runnable hook, boolean override) {
		checkActive();
		config.addStartupHook(name, hook, override);
		for(ViNode vh: hosts) {
			vh.addStartupHook(name, hook, override);
		}
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public synchronized void addShutdownHook(String name, Runnable hook, boolean override) {
		checkActive();
		config.addShutdownHook(name, hook, override);
		for(ViNode vh: hosts) {
			vh.addShutdownHook(name, hook, override);
		}
	}
	
	@Override
	public synchronized void addStartupHook(String name, Runnable hook) {
		checkActive();
		config.addStartupHook(name, hook);
		for(ViNode vh: hosts) {
			vh.addStartupHook(name, hook);
		}
	}
	
	@Override
	public synchronized void addShutdownHook(String name, Runnable hook) {
		checkActive();
		config.addShutdownHook(name, hook);
		for(ViNode vh: hosts) {
			vh.addShutdownHook(name, hook);
		}
	}
	
	@Override
	public synchronized void suspend() {
		checkActive();
		
	}

	@Override
	public synchronized void resume() {
		checkActive();
		
	}

	@Override
	public void kill() {
		if (!shutdown) {
			for(ViNode host: hosts) {
				host.kill();
			}			
			shutdown = true;
		}		
	}

	@Override
	public synchronized void shutdown() {
		if (!shutdown) {
			for(ViNode host: hosts) {
				host.shutdown();
			}			
			shutdown = true;
		}		
	}

	@Override
	public void touch() {
		exec(new Touch());		
	}

	@Override
	public synchronized void exec(Runnable task) {
		MassExec.waitAll(massSubmit(task));		
	}
	
	@Override
	public synchronized void exec(VoidCallable task) {
		MassExec.waitAll(massSubmit(task));		
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized <T> T exec(Callable<T> task) {
		return (T) MassExec.waitAll((List)massSubmit(task)).get(0);		
	}
	
	@Override
	public synchronized Future<Void> submit(Runnable task) {
		return new GroupFuture<Void>(massSubmit(task));
	}
	
	@Override
	public synchronized Future<Void> submit(VoidCallable task) {
		return new GroupFuture<Void>(massSubmit(task));
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized <T> Future<T> submit(Callable<T> task) {
		return new GroupFuture(massSubmit(task));
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized <T> List<T> massExec(Callable<? extends T> task) {
		return MassExec.waitAll((List)massSubmit(task));
	}
	
	@Override
	public synchronized List<Future<Void>> massSubmit(Runnable task) {
		checkExecutable();
		List<Future<Void>> results = new ArrayList<Future<Void>>();
		for(ViNode host: hosts) {
			results.addAll(host.massSubmit(task));
		}
		return results;
	}
	
	@Override
	public synchronized List<Future<Void>> massSubmit(VoidCallable task) {
		checkExecutable();
		List<Future<Void>> results = new ArrayList<Future<Void>>();
		for(ViNode host: hosts) {
			results.addAll(host.massSubmit(task));
		}
		return results;
	}
	
	@Override
	public synchronized <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		checkExecutable();
		List<Future<T>> results = new ArrayList<Future<T>>();
		for(ViNode host: hosts) {
			results.addAll(host.massSubmit(task));
		}
		return results;
	}
	
	private static class GroupFuture<T> implements Future<T> {
		
		private List<Future<T>> futures;
		
		public GroupFuture(List<Future<T>> futures) {
			this.futures = futures;
		}

		@Override
		public boolean cancel(boolean mayInterrupt) {
			for(Future<T> future : futures) {
				try {
					future.cancel(mayInterrupt);
				}
				catch(RuntimeException e) {
					// ignore;
				}
			}
			return true;
		}

		@Override
		public boolean isCancelled() {
			// TODO implement isCancelled
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isDone() {
			// TODO implement isDone
			throw new UnsupportedOperationException();
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public T get() throws InterruptedException, ExecutionException {
			return (T) MassExec.waitAll((List)futures).get(0);
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			// TODO implement get() with timeout
			throw new UnsupportedOperationException();
		}
	}
	
	private static class Touch implements Runnable, Serializable {

		private static final long serialVersionUID = 20121116L;

		@Override
		public void run() {
		}
	}
}
