package org.gridkit.vicluster.spi;

/**
 *
 */
public interface SpiFactory {

	public Object instantiate(ViCloudContext context, String attrName, AttrBag config);
	
}
