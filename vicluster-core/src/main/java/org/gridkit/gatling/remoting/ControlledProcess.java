package org.gridkit.gatling.remoting;

import java.util.concurrent.ExecutorService;

public interface ControlledProcess {

	public Process getProcess();
	
	public ExecutorService getExecutionService();
	
}
