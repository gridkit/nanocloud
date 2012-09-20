package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.ViNodeProvider;

public interface ViSpiConsts {

	public final static String ID = AttrBag.ID;
	public final static String NAME = AttrBag.NAME;
	public final static String TYPE = AttrBag.TYPE;
	public final static String LABEL = AttrBag.LABEL;
	public final static String INSTANCE = AttrBag.INSTANCE;	
	public final static String EXECUTOR_PROVIDER = "executor-provider";
	
	public static String NODE_NAME = "node-name";
	public static String NODE_TYPE = "vinode";
	
	public static String NODE_PROVIDER = "node-provider";
	public static String NODE_PROVIDER_TYPE = ViNodeProvider.class.getName();
	
	public static String JVM_FACTORY = "jvm-factory";
	public static final String IMPL_CLASS = "implementation-class";
	
}
