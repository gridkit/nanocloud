package org.gridkit.nanocloud.telecontrol.isolate;

import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViConfigurable.Delegate;
import org.gridkit.vicluster.isolate.IsolateProps;

public class IsolateConfig extends Delegate {

	private ViConfigurable conf;

	public static IsolateConfig at(ViConfigurable conf) {
		return new IsolateConfig(conf);
	}
	
	public IsolateConfig(ViConfigurable conf) {
		this.conf = conf;
	}

	@Override
	protected ViConfigurable getConfigurable() {
		return conf;
	}

	public IsolateConfig shareClass(Class<?> c) {
		shareClass(c.getName());
		return this;
	}

	public IsolateConfig shareClass(String name) {
		conf.setProp(IsolateProps.SHARE_CLASS + name, name);
		return this;
	}

	public IsolateConfig sharePackage(String name) {
		conf.setProp(IsolateProps.SHARE_PACKAGE + name, name);
		return this;
	}

	public IsolateConfig isolateClass(Class<?> c) {
		isolateClass(c.getName());
		return this;
	}
	
	public IsolateConfig isolateClass(String name) {
		conf.setProp(IsolateProps.ISOLATE_CLASS + name, name);
		return this;
	}
	
	public IsolateConfig isolatePackage(String name) {
		conf.setProp(IsolateProps.ISOLATE_PACKAGE + name, name);
		return this;
	}
}
