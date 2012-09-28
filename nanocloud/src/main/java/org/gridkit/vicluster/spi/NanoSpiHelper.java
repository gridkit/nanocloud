package org.gridkit.vicluster.spi;


public class NanoSpiHelper {

	public static AttrBag configureJvm(ViCloudContext context, AttrBag nodeInfo) {		
		String name = nodeInfo.getLast(AttrBag.NAME);
		String type = JvmProcessConfiguration.class.getName();

		AttrList proto = new AttrList();
		proto.add(AttrBag.NAME, name);
		proto.add(AttrBag.TYPE, type);
		NodeSpiHelper.propagateAttribs(nodeInfo, proto);
		
		AttrBag bag = context.ensureResource(Selectors.name(name, type), proto);
		return bag;
	};

	public static AttrBag configureHost(ViCloudContext context, AttrBag nodeInfo) {
		String name = nodeInfo.getLast(RemoteAttrs.NODE_HOST);
		if (name == null) {
			name = nodeInfo.getLast(AttrBag.NAME);
		}
		String type = HostConfiguration.class.getName();

		AttrList proto = new AttrList();
		proto.add(AttrBag.NAME, name);
		proto.add(AttrBag.TYPE, type);
		NodeSpiHelper.propagateAttribs(nodeInfo, proto);
		
		AttrBag bag = context.ensureResource(Selectors.name(name, type), proto);
		return bag;		
	};
	
}
