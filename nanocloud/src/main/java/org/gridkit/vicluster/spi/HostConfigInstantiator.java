package org.gridkit.vicluster.spi;

public class HostConfigInstantiator implements SpiFactory {

	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		HostConfiguration configuration = new HostConfiguration();
		String hostname = config.getLast(RemoteAttrs.HOST_HOSTNAME);
		String login = config.getLast(RemoteAttrs.HOST_LOGIN);
		String password = config.getLast(RemoteAttrs.HOST_PASSWORD);
		String privateKeyPath = config.getLast(RemoteAttrs.HOST_PRIVATE_KEY_PATH);
		
		configuration.setHostname(hostname);
		configuration.setLogin(login);
		configuration.setPassword(password);
		configuration.setPrivateKeyPath(privateKeyPath);
		
		return configuration;
	}
}
