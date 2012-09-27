package org.gridkit.vicluster.spi;

public class RemoteHostResolver implements SpiFactory {

	public static final String HOST = "node.host";
	public static final String HOSTGROUP = "node.hostgroup";
	public static final String COLOCATION_ID = "node.colocation-id";
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		String hostgroup = 
		
		AttrBag hostConfig = NanoSpiHelper.configureHost(context, config);
		HostConfiguration hconfig = hostConfig.getInstance(HostConfiguration.class);

		if (hconfig.getHostname() == null) {
			throw new IllegalArgumentException("No hostname defined for " + config.getLast(AttrBag.NAME));
		}
		
		AttrList attributes = new AttrList();
		attributes.add(AttrBag.TYPE, Host.class.getName());
		attributes.add(AttrBag.NAME, hconfig.getHostname());
		if (hconfig.getLogin() != null) {
			attributes.add(HostConfigInstantiator.LOGIN, hconfig.getLogin());
		}

		AttrBag bean = context.ensureResource(attributes);
		
		return bean.getInstance(Host.class); 		
	}
	
}
