package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.telecontrol.JvmProcessFactory;

public interface Host {
	
	public String getHostname();
	
	public JvmProcessFactory getProcessFactory();
	
}
