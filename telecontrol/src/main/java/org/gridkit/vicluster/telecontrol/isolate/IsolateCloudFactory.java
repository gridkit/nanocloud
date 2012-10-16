package org.gridkit.vicluster.telecontrol.isolate;

import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;

public class IsolateCloudFactory {

	public static ViManager createCloud(String... packages) {
		
		IsolateJvmNodeFactory factory = new IsolateJvmNodeFactory(packages);
		JvmNodeProvider provider = new JvmNodeProvider(factory);
		
		return new ViManager(provider);		
	}
	
}
