package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;

public interface CoreNode {

	public AdvancedExecutor getExecutor();
	
	
	
	public void shutdown();
	
}
