package org.gridkit.vicluster.telecontrol.spi;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public interface SshConnector {

	public Session connect() throws JSchException;
	
}
