package org.gridkit.vicluster;

/**
 * Normally ViNode start/shutdown hooks are to be executed inside of ViNode.
 * In certain cases though, host side execution is desirable.
 * Any hook implementing this interface are going to be executed on host.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface HostSideHook extends Runnable {

	/**
	 * @param shutdown @true - startup, @false - shutdown
	 */
	public void hostRun(boolean shutdown);
	
}
