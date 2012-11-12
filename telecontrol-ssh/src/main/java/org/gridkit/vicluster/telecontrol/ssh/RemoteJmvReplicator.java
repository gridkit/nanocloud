package org.gridkit.vicluster.telecontrol.ssh;

import java.util.Map;

import org.gridkit.vicluster.telecontrol.JvmProcessFactory;

public interface RemoteJmvReplicator extends JvmProcessFactory {

	/**
	 * @param properties - target ViNode config
	 */
	public void configure(Map<String, String> nodeConfig);
	
	/**
	 * @return configuration finger prints, which could be used to pool and reuse replicators
	 */
	public String getFingerPrint();
	
	public void init() throws Exception;
	
	public void dispose();
	
}
