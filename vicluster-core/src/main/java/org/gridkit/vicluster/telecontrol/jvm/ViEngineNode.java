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
package org.gridkit.vicluster.telecontrol.jvm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.vicluster.Hooks;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.ViExtender;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ViEngineNode implements ViNode {

	private ViEngine engine;
	private ViExecutor execProxy;
	
	public ViEngineNode(ViEngine engine) {
		this.engine = engine;
		
		ManagedProcess mp = engine.getConfig().getManagedProcess();
		if (mp == null) {
			throw new RuntimeException("Cannot create node, ManagedProcess is not available");
		}
		execProxy = new ExecProxy(mp.getExecutionService());
		
	}

	@Override
	public <X> X x(ViExtender<X> extention) {
		return extention.wrap(this);
	}
	
	@Override
	public void touch() {
		// do nothing
	}
	
	public void exec(Runnable task) {
		execProxy.exec(task);
	}

	public void exec(VoidCallable task) {
		execProxy.exec(task);
	}

	public <T> T exec(Callable<T> task) {
		return execProxy.exec(task);
	}

	public Future<Void> submit(Runnable task) {
		return execProxy.submit(task);
	}

	public Future<Void> submit(VoidCallable task) {
		return execProxy.submit(task);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return execProxy.submit(task);
	}

	public <T> List<T> massExec(Callable<? extends T> task) {
		return execProxy.massExec(task);
	}

	public List<Future<Void>> massSubmit(Runnable task) {
		return execProxy.massSubmit(task);
	}

	public List<Future<Void>> massSubmit(VoidCallable task) {
		return execProxy.massSubmit(task);
	}

	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		return execProxy.massSubmit(task);
	}

	@Override
	public void setProp(final String propName, final String value) {
		setProps(Collections.singletonMap(propName, value));
	}

	@Override
	public void setProps(Map<String, String> props) {
		for(String p: props.keySet()) {
			if (!ViConf.isVanilaProp(p)) {
				throw new IllegalArgumentException("[" + p + "] is not 'vanila' prop");
			}
		}
		final Map<String, String> copy = new LinkedHashMap<String, String>(props);
		exec(new Runnable() {
			@Override
			public void run() {
				for(String name: copy.keySet()) {
					System.setProperty(name, copy.get(name));
				}
			}
		});
	}

	@Override
	public void setConfigElement(String key, Object value) {
		engine.setPragmas(Collections.singletonMap(key, value));
	}

	@Override
	public void setConfigElements(Map<String, Object> config) {
		engine.setPragmas(config);
	}

	@Override
	public void addStartupHook(String name, Runnable hook) {
		setConfigElement(ViConf.HOOK + name, new Hooks.StratupHook(hook));
	}

	@Override
	@Deprecated
	public void addStartupHook(String name, Runnable hook, boolean override) {
		setConfigElement(ViConf.HOOK + name, new Hooks.StratupHook(hook));
	}

	@Override
	public synchronized void addShutdownHook(String name, Runnable hook) {
		setConfigElement(ViConf.HOOK + name, new Hooks.ShutdownHook(hook));
	}

	@Override
	@Deprecated
	public synchronized void addShutdownHook(String name, Runnable hook, boolean override) {
		setConfigElement(ViConf.HOOK + name, new Hooks.ShutdownHook(hook));
	}

	@Override
	public String getProp(final String propName) {
		return (String)engine.getPragma(propName);
	}

	@Override
	public void suspend() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void kill() {
		engine.kill();
	}

	@Override
	public void shutdown() {
		engine.shutdown();
	}


	private class ExecProxy extends AdvExecutor2ViExecutor {

		public ExecProxy(AdvancedExecutor advExec) {
			super(advExec);
		}

		@Override
		public void touch() {
		}

		@Override
		protected AdvancedExecutor getExecutor() {
			return super.getExecutor();
		}
	}
}
