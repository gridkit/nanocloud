package org.gridkit.vicluster.spi;

class CopyAttr implements SpiFactory {

	private final String sourceAttr;
	
	public CopyAttr(String sourceAttr) {
		this.sourceAttr = sourceAttr;
	}

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		return config.getLast(sourceAttr);
	}
}
