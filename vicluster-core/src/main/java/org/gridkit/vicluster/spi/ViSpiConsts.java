package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;

public interface ViSpiConsts {

	public final static String ID = AttrBag.ID;
	public final static String NAME = AttrBag.NAME;
	public final static String TYPE = AttrBag.TYPE;
	public final static String LABEL = AttrBag.LABEL;
	public final static String INSTANCE = AttrBag.INSTANCE;	
	public final static String EXECUTOR_PROVIDER = "executor-provider";
	
	public static String NODE_TYPE = ViNodeSpi.class.getName();
	public static String EXECUTOR_PROVIDER_TYPE = ExecutorProvider.class.getName();
	public static String EXECUTOR_TYPE = AdvancedExecutor.Component.class.getName();	
		
	public static String JVM_FACTORY = "jvm-factory";
	public static final String IMPL_CLASS = "implementation-class";
	
}
