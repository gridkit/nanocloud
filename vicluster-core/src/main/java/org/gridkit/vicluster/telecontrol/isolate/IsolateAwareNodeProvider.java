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
package org.gridkit.vicluster.telecontrol.isolate;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViConfExtender;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeExtender;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.gridkit.vicluster.isolate.IsolateSelfInitializer;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.ExecCommand;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;

@SuppressWarnings("deprecation")
public class IsolateAwareNodeProvider extends JvmNodeProvider {

	public IsolateAwareNodeProvider() {
		super(null);
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		try {
			Map<String, String> isolateProps = config.getAllProps(IsolateProps.PREFIX);

			for(String key: config.getAllProps(JvmProps.CP_ADD).keySet()) {
				String path = config.getProp(key);
				if (path == null) {
					continue;
				}
				if ("".equals(path)) {
					path = key.substring(JvmProps.CP_ADD.length());
				}
				isolateProps.put(IsolateProps.CP_INCLUDE + new File(path).toURI().toString(), "");
			}
			for(String key: config.getAllProps(JvmProps.CP_REMOVE).keySet()) {				
				String path = config.getProp(key);
				if (path == null) {
					continue;
				}
				if ("".equals(path)) {
					path = key.substring(JvmProps.CP_ADD.length());
				}
				String url = new File(path).toURI().toString();
				isolateProps.put(IsolateProps.CP_EXCLUDE + url, "");
				isolateProps.put(IsolateProps.CP_EXCLUDE + "jar:" + url + "!/", "");
			}

			IsolateJvmNodeFactory factory = new IsolateJvmNodeFactory(isolateProps, config.getAllVanilaProps(), BackgroundStreamDumper.SINGLETON);
			JvmConfig jvmConfig = prepareJvmConfig(config);
			ManagedProcess process = factory.createProcess(name, jvmConfig);
			return createViNode(name, config, process);
		} catch (IOException e) {
			// TODO special exception for node creation failure
			throw new RuntimeException("Failed to create node '" + name + "'", e);
		}		
	}
	
	@Override
	protected ViNode createViNode(String name, ViNodeConfig config, ManagedProcess process) throws IOException {
		Map<String, String> isolateProps = config.getAllProps(IsolateProps.PREFIX);
		// add Isolate init hook first
		ViNodeConfig cc = new ViNodeConfig();
		cc.x(VX.HOOK).setStartupHook("isolate-init-hook", new IsolateSelfInitializer(isolateProps));
		config.apply(cc);
		
		return new WrapperNode(super.createViNode(name, cc, process));
	}
	
	static class IsolateJvmNodeFactory extends LocalJvmProcessFactory {

		private Map<String, String> isolateConfigProps;
		private Map<String, String> vanilaProps;


		private IsolateJvmNodeFactory(Map<String, String> isolateConfigProps, Map<String, String> vanilaProps, StreamCopyService streamCopyService) {
			super(streamCopyService);
		    this.isolateConfigProps = isolateConfigProps;
			this.vanilaProps = vanilaProps;
		}

		@Override
		protected Process startProcess(String name, ExecCommand jvmCmd) throws IOException {
			return new IsolateProcess(name, isolateConfigProps, vanilaProps, jvmCmd);
		}
	}
	
	
	private static class WrapperNode implements ViNode {
		
		private final ViNode node;

		public WrapperNode(ViNode node) {
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
		
		public void setProp(String propName, String value) {
			node.setProp(propName, value);
			if (propName.startsWith(IsolateProps.PREFIX)) {
				Map<String, String> map = Collections.singletonMap(propName, value);
				node.exec(new IsolateSelfInitializer(map));
			}
		}

		public String getProp(String propName) {
			return node.getProp(propName);
		}

		@Override
		public Object getPragma(String pragmaName) {
			return node.getPragma(pragmaName);
		}

		public void setConfigElement(String key, Object value) {
			node.setConfigElement(key, value);
		}

		public void setConfigElements(Map<String, Object> config) {
			node.setConfigElements(config);
		}

		public void setProps(Map<String, String> props) {
			node.setProps(props);
			Map<String, String> map = new LinkedHashMap<String, String>();
			for(String key: props.keySet()) {
				if (key.startsWith(IsolateProps.PREFIX)) {
					map.put(key, props.get(key));
				}
				node.exec(new IsolateSelfInitializer(map));
			}
		}

		public void touch() {
			node.touch();
		}

		@Override
		public void kill() {
			node.kill();
		}

		public void shutdown() {
			node.shutdown();
		}

		public void exec(Runnable task) {
			node.exec(task);
		}

		public void exec(VoidCallable task) {
			node.exec(task);
		}

		public <T> T exec(Callable<T> task) {
			return node.exec(task);
		}

		public Future<Void> submit(Runnable task) {
			return node.submit(task);
		}

		public Future<Void> submit(VoidCallable task) {
			return node.submit(task);
		}

		public <T> Future<T> submit(Callable<T> task) {
			return node.submit(task);
		}

		public <T> List<T> massExec(Callable<? extends T> task) {
			return node.massExec(task);
		}

		public List<Future<Void>> massSubmit(Runnable task) {
			return node.massSubmit(task);
		}

		public List<Future<Void>> massSubmit(VoidCallable task) {
			return node.massSubmit(task);
		}

		public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
			return node.massSubmit(task);
		}
	}
}
