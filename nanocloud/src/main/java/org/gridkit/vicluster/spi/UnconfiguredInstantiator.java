package org.gridkit.vicluster.spi;

public class UnconfiguredInstantiator implements SpiFactory {

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		throw new IllegalArgumentException("Field " + attrName + " is not configured. Bean: " + config);
	}
}
