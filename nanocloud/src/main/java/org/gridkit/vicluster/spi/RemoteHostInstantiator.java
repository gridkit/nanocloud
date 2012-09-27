package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.telecontrol.spi.SimpleSshConnector;
import org.gridkit.vicluster.telecontrol.spi.SshControlledHost;

public class RemoteHostInstantiator implements SpiFactory {

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		String hostname = config.getLast(RemoteAttrs.HOST_HOSTNAME);
		String login = config.getLast(RemoteAttrs.HOST_LOGIN);
		String password = config.getLast(RemoteAttrs.HOST_PASSWORD);
		String keyFile = config.getLast(RemoteAttrs.HOST_PRIVATE_KEY_PATH);

		String agentHome = config.getLast(RemoteAttrs.HOST_AGENT_HOME);
		String defaultJava = config.getLast(RemoteAttrs.HOST_DEFAULT_JAVA);
		
		SimpleSshConnector connector = new SimpleSshConnector();
		connector.setHostname(hostname);
		connector.setAccount(login);
		if (password != null) {
			connector.setPassword(password);
		}
		if (keyFile != null) {
			connector.setKeyFile(keyFile);
		}
		
		SshControlledHost sshHost = new SshControlledHost(hostname, connector);
		
		if (agentHome != null) {
			sshHost.setAgentHome(agentHome);
		}
		if (defaultJava != null) {
			sshHost.setDefaultJava(defaultJava);
		}
		
		return sshHost;
	}
}
