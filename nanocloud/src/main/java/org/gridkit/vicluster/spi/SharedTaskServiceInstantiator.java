package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.SensibleTaskService;

public class SharedTaskServiceInstantiator implements SpiFactory {

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		return SensibleTaskService.getShareInstance();
	}
}
