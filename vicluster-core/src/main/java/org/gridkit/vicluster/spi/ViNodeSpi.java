package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;

public interface ViNodeSpi {
	
	public boolean isActive();
	
	public AdvancedExecutor getExecutor();
	
	public void addPreShutdownAction(ViNodeAction action);

	public void addPostShutdownAction(ViNodeAction action);

	public void shutdown();

}
