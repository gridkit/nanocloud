package org.gridkit.fabric.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public interface SshSessionProvider {

	public Session getSession(String host) throws JSchException;
	
}
