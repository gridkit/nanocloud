package org.gridkit.vicluster.spi;

import java.io.IOException;

import org.gridkit.vicluster.telecontrol.ControlledProcess;

public interface Host {
	
	public String getHostname();
	
	public boolean verify();
	
	public ControlledProcess startProcess(JvmProcessConfiguration configuration) throws IOException;
	
}
