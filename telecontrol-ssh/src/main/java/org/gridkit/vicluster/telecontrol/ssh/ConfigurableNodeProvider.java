package org.gridkit.vicluster.telecontrol.ssh;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.isolate.IsolateViNodeProvider;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;

public class ConfigurableNodeProvider implements ViNodeProvider {

	private IsolateViNodeProvider isolateProvider;
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

	private synchronized ViNodeProvider getIsolateProvider() {
		if (isolateProvider == null) {
			isolateProvider = new IsolateViNodeProvider(); 
		}
		return isolateProvider;
	}

	private ViNodeProvider getLocalProvider() {
		if (localProvider == null) {
			localProvider = new JvmNodeProvider(new LocalJvmProcessFactory());
		}
		return localProvider;
	}
	
	private ViNodeProvider getRemoteProvider() {
		if (remoteProvider == null) {
			remoteProvider = new ConfigurableSshReplicator();
		}
		return remoteProvider;
	}
}
