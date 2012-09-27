package org.gridkit.vicluster.spi;

import java.util.List;

public class JvmConfigInstantiator implements SpiFactory {

	public static final String JVM_PATH = "jvm-path";
	public static final String JVM_OPTION = "jvm-option";
	public static final String JVM_WORK_PATH = "jvm-work-path";
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		String name = config.getLast(AttrBag.NAME);
		String jvmPath = config.getLast(JVM_PATH);
		String jvmWorkPath = config.getLast(JVM_WORK_PATH);
		List<String> jvmOptions = config.getAllInOrder(JVM_OPTION);
		
		JvmProcessConfiguration jvmConfig = new JvmProcessConfiguration();
		jvmConfig.setName(name);
		
		if (jvmPath != null) {
			jvmConfig.setJvmPath(jvmPath);
		}
		if (jvmWorkPath != null) {
			jvmConfig.setJvmPath(jvmPath);
		}
		jvmConfig.addJvmOptions(jvmOptions);
		
		return jvmConfig;		
	}
}
