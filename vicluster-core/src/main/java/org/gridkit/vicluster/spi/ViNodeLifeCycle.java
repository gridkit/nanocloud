package org.gridkit.vicluster.spi;

public interface ViNodeLifeCycle extends ViNodeSPI {
	
	public void addPostInitAction(ViNodeAction action);

	public void getPreShutdownAction(ViNodeAction action);

}
