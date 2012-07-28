package org.gridkit.gatling.firegrid;

import java.io.Serializable;

public interface WorkExecutor extends Serializable {

	public void initialize();

	public void initWorkPacket(WorkPacket packet);
	
	public int execute();
	
	public void finishWorkPacket(WorkPacket packet);
	
	public void shutdown();
	
}
