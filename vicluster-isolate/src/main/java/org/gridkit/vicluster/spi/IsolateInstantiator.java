package org.gridkit.vicluster.spi;

import java.util.List;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutor.Component;
import org.gridkit.vicluster.isolate.Isolate;

public class IsolateInstantiator implements SpiFactory {

	public static final String ISOLATE = "isolate";
	
	public static final String NAME = AttrBag.NAME;
	public static final String ISOLATE_NAME = "isolate-name";
	public static final String PACKAGE = "package";
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		String name = config.getLast(ISOLATE_NAME);
		if (name == null) {
			name = config.getLast(NAME);
		}
		Isolate isolate = new Isolate(name);
		List<String> packages = config.getAll(PACKAGE); 
		for(String prefix: packages) {
			isolate.addPackage(prefix);
		}
		
		return isolate;
	}
}
