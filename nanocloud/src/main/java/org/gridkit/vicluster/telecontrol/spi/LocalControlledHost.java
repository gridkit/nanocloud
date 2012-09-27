package org.gridkit.vicluster.telecontrol.spi;

import java.io.IOException;

import org.gridkit.vicluster.spi.Host;
import org.gridkit.vicluster.spi.JvmProcessConfiguration;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;

public class LocalControlledHost implements Host {

	private LocalJvmProcessFactory processFactory = new LocalJvmProcessFactory();
	
	@Override
	public String getHostname() {
		return "localhost";
	}

	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public ControlledProcess startProcess(JvmProcessConfiguration configuration) throws IOException {
		String name = configuration.getName();
		if (name == null) {
			name = "no-name";
		}
		
		// ignoring JVM path
		
		JvmConfig jconfig = new JvmConfig();
		for(String option: configuration.getJvmOptions()) {
			jconfig.addOption(option);
		}
		
		return processFactory.createProcess(name, jconfig);
	}
}
