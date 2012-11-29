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
package org.gridkit.vicluster.telecontrol.ssh;

import org.gridkit.vicluster.InProcessViNodeProvider;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.isolate.IsolateJvmNodeFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;

public class ConfigurableNodeProvider implements ViNodeProvider {

	private ViNodeProvider inprocessProvider;
	private ViNodeProvider isolateProvider;
	private ViNodeProvider localProvider;
	private ViNodeProvider remoteProvider;
	private boolean localOnly;
	
	public ConfigurableNodeProvider(boolean localOnly) {
		this.localOnly = localOnly;
	}
	
	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		String type = config.getProp(ViProps.NODE_TYPE);
		if (ViProps.NODE_TYPE_ISOLATE.equals(type)) {
			return getIsolateProvider().createNode(name, config);
		}
		else if (ViProps.NODE_TYPE_IN_PROCESS.equals(type)) {
			return getInprocessProvider().createNode(name, config);
		}
		else if (localOnly || ViProps.NODE_TYPE_LOCAL.equals(type)) {
			return getLocalProvider().createNode(name, config);
		}
		else if (ViProps.NODE_TYPE_REMOTE.equals(type)) {
			return getRemoteProvider().createNode(name, config);
		}
		else {
			throw new IllegalArgumentException("Unknown node type '" + type + "' for node '" + name + "'");
		}
	}

	private synchronized ViNodeProvider getInprocessProvider() {
		if (inprocessProvider == null) {
			inprocessProvider = new InProcessViNodeProvider();
		}
		return inprocessProvider;
	}

	private synchronized ViNodeProvider getIsolateProvider() {
		if (isolateProvider == null) {
			isolateProvider = new JvmNodeProvider(new IsolateJvmNodeFactory());
		}
		return isolateProvider;
	}

	private synchronized ViNodeProvider getLocalProvider() {
		if (localProvider == null) {
			localProvider = new JvmNodeProvider(new LocalJvmProcessFactory());
		}
		return localProvider;
	}
	
	private synchronized ViNodeProvider getRemoteProvider() {
		if (remoteProvider == null) {
			remoteProvider = new ConfigurableSshReplicator();
		}
		return remoteProvider;
	}
}
