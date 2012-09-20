package org.gridkit.vicluster.spi;

public class ReflectionInstantiator implements SpiFactory {

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		String className = config.getLast(ViSpiConsts.IMPL_CLASS);
		if (className == null) {
			throw new NullPointerException("Missing class name attribute. Config: " + config);
		}
		try {
			Class<?> c = Class.forName(className);		
			Object obj = c.newInstance();
			return obj;
		}
		catch(Exception e) {
			throw new RuntimeException("Failed instantiate '" + attrName + "' from " + config);
		}
	}
}
