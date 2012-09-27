package org.gridkit.vicluster.spi;

public class HostConfigInstantiator implements SpiFactory {

	public static final String HOSTNAME = "host.hostname";
	public static final String LOGIN = "host.login";
	public static final String PASSWORD = "host.password";
	public static final String PRIVATE_KEY_PATH = "host.private-key-path";
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		HostConfiguration configuration = new HostConfiguration();
		String hostname = config.getLast(HOSTNAME);
		String login = config.getLast(LOGIN);
		String password = config.getLast(PASSWORD);
		String privateKeyPath = config.getLast(PRIVATE_KEY_PATH);
		
		configuration.setHostname(hostname);
		configuration.setLogin(login);
		configuration.setPassword(password);
		configuration.setPrivateKeyPath(privateKeyPath);
		
		return configuration;
	}
}
