package org.gridkit.nanocloud;

import org.gridkit.vicluster.ViProps;

public class IsolateCloudFactoryTest extends CloudFactoryTest{

	public Cloud initCloud() {
		Cloud cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setIsolateType();
		return cloud;
	}	
}
