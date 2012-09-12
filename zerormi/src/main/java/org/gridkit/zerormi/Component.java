package org.gridkit.zerormi;

public interface Component {
	
	public boolean isInitalized();
	
	public boolean isTerminated();
	
	/**
	 * This is meant for ease of diagnostic.
	 * In case of termination of fatal failure
	 * component is meant to retain key error description.
	 * 
	 * This way superviser of failing circuit could provide
	 * reasonable failure description.
	 */
	public String getStatusLine();
	
	public void shutdown();

}
