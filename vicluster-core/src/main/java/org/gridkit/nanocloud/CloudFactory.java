package org.gridkit.nanocloud;

import org.gridkit.nanocloud.telecontrol.LocalNodeTypeHandler;
import org.gridkit.nanocloud.telecontrol.isolate.IsolateNodeTypeHandler;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConf.ConsoleConf;
import org.gridkit.vicluster.ViEngine.InductiveRule;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.jvm.ViEngineNodeProvider;

public class CloudFactory {	

	public static final String PROP_TYPE_HANDLER = "org.gridkit.nanocloud.type-handler.";
	
	public static Cloud createCloud() {
		ViManager cloud = new ViManager(new ViEngineNodeProvider());
		initDefaultTypeHandlers(cloud);
		ConsoleConf.at(cloud.node("**")).echoPrefix("~[%s] !(.*)");
		return cloud;
	}
	
	public static void addType(Cloud cloud, String type, InductiveRule typeHandler) {
		cloud.node("**").setConfigElement(ViConf.TYPE_HANDLER + type, typeHandler);
	}

	public static void addType(Cloud cloud, String type, String className) {
		InductiveRule rule;
		try {
			Class<?> c = Class.forName(className);
			Object i = c.newInstance();
			rule = (InductiveRule) i;
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		cloud.node("**").setConfigElement(ViConf.TYPE_HANDLER + type, rule);
	}

	protected static void initDefaultTypeHandlers(Cloud cloud) {
		initInProcessTypeHandler(cloud);
		initIsolateTypeHandler(cloud);
		initLocalTypeHandler(cloud);
		initRemoteTypeHandler(cloud);
		initConfiguredTypeHandlers(cloud);
	}

	private static void initInProcessTypeHandler(Cloud cloud) {
		// TODO Implement		
	}

	private static void initIsolateTypeHandler(Cloud cloud) {
		addType(cloud, ViConf.NODE_TYPE__ISOLATE, new IsolateNodeTypeHandler());		
	}

	private static void initLocalTypeHandler(Cloud cloud) {
		addType(cloud, ViConf.NODE_TYPE__LOCAL, new LocalNodeTypeHandler());
	}

	private static void initRemoteTypeHandler(Cloud cloud) {
		try {
			addType(cloud, ViConf.NODE_TYPE__REMOTE, "org.gridkit.nanocloud.telecontrol.ssh.RemoteNodeTypeHandler");
		}
		catch(RuntimeException e) {
			// ignore
		}
	}

	/**
	 * Add type handles configure via system properties.
	 */
	protected static void initConfiguredTypeHandlers(Cloud cloud) {
		for(Object k: System.getProperties().keySet()) {
			String key = (String) k;
			if (key.startsWith(PROP_TYPE_HANDLER)) {
				String type = key.substring(PROP_TYPE_HANDLER.length());
				String th = System.getProperty(key);
				if (th == null || th.trim().length() == 0) {
					addType(cloud, type, (InductiveRule)null);
				}
				else {
					try {
						addType(cloud, type, type);
					}
					catch(RuntimeException e) {
						// ignore
					}
				}
			}
		}
	}
}
