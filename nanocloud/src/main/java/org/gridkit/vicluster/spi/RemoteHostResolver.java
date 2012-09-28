package org.gridkit.vicluster.spi;

public class RemoteHostResolver implements SpiFactory {

	public static final String HOST = "node.host";
	public static final String HOSTGROUP = RemoteAttrs.NODE_HOSTGROUP;
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		
		String hostgroup = config.getLast(HOSTGROUP);
		if (hostgroup != null) {
			HostGroup group = context.ensureNamedInstance(hostgroup, HostGroup.class);
			if (group == null) {
				throw new IllegalArgumentException("Unknown host group: " + hostgroup);
			}
			return group.resolveHost(config);
		}
		else {
		
			AttrBag hostConfig = NanoSpiHelper.configureHost(context, config);
			HostConfiguration hconfig = hostConfig.getInstance(HostConfiguration.class);
	
			if (hconfig.getHostname() == null) {
				throw new IllegalArgumentException("No hostname defined for " + config.getLast(AttrBag.NAME));
			}
			
			AttrList attributes = new AttrList();
			attributes.add(AttrBag.TYPE, Host.class.getName());
			attributes.add(AttrBag.NAME, hconfig.getHostname());
			if (hconfig.getLogin() != null) {
				attributes.add(RemoteAttrs.HOST_LOGIN, hconfig.getLogin());
			}
	
			AttrBag bean = context.ensureResource(attributes);
			
			return bean.getInstance(Host.class);
		}
	}
	
}
