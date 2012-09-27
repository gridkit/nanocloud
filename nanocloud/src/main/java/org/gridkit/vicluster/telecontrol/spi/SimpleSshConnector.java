package org.gridkit.vicluster.telecontrol.spi;

import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SimpleSshConnector implements SshConnector {

	private SimpleSshSessionProvider provider = new SimpleSshSessionProvider();
	private String hostname;
	
	public SimpleSshConnector() {		
	}
	
	@Override
	public Session connect() throws JSchException {
		return provider.getSession(hostname, null);
	}

	public void setHostname(String hostname) {
		if (hostname.contains("@")) {
			int n = hostname.indexOf('@');
			String account = hostname.substring(0, n);
			setAccount(account);
			hostname = hostname.substring(n + 1);
		}
		this.hostname = hostname;
	}
	
	public void setAccount(String account) {
		provider.setUser(account);
	}
	
	public void setPassword(String password) {
		provider.setPassword(password);
	}
	
	public void setKeyFile(String path) {
		provider.setKeyFile(path);
	}
	
	@Override
	public String toString() {
		return provider.getUser() + "@" + hostname;
	}
}
