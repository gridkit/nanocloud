package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.telecontrol.spi.LocalControlledHost;

public class LocalHostResolver implements SpiFactory {

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		LocalControlledHost host = context.ensureNamedInstance("default", LocalControlledHost.class);
		return host;
	}
}
