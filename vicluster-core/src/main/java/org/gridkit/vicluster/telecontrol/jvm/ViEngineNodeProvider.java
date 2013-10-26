package org.gridkit.vicluster.telecontrol.jvm;

import java.util.Map;

import org.gridkit.nanocloud.telecontrol.ConsoleOutputSupport;
import org.gridkit.vicluster.AbstractCloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;

public class ViEngineNodeProvider implements ViNodeProvider {

	protected Context context = new Context();
	
	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		Map<String, Object> engineConfig = config.getConfigMap();
		engineConfig.put(ViConf.NODE_NAME, name);
		engineConfig.put("vinode.name", name);
		initConfigDefaults(engineConfig);
		ViEngine.Core core = new ViEngine.Core();
		core.ignite(engineConfig);
		ViEngineNode node = new ViEngineNode(core);
		return node;
	}

	@Override
	public void shutdown() {
		context.runFinalizers();
		
	}

	protected void setDefault(Map<String, Object> engineConfig, String key, Object value) {
		if (!engineConfig.containsKey(key)) {
			engineConfig.put(key, value);
		}
	}

	protected void initConfigDefaults(Map<String, Object> engineConfig) {
		setDefault(engineConfig, ViConf.SPI_CLOUD_CONTEXT, context);
		
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__CONSOLE, new ConsoleOutputSupport());
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__CLASSPATH, new ViEngine.InitTimePragmaHandler());
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__HOOK, new ViEngine.InitTimePragmaHandler());
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__JVM,  new ViEngine.InitTimePragmaHandler());
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__NODE,  new ViEngine.InitTimePragmaHandler());
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__RUNTIME,  new ViEngine.ReadOnlyPragmaHandler());
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__REMOTE,  new ViEngine.InitTimePragmaHandler());
		
		setDefault(engineConfig, ViConf.CONSOLE_STD_OUT_ECHO, "true");
		setDefault(engineConfig, ViConf.CONSOLE_STD_ERR_ECHO, "true");

		setDefault(engineConfig, ViConf.HOOK_NODE_INITIALIZER, new ViEngine.DefaultInitRuleSet());
	}

	private final class Context extends AbstractCloudContext {

		@Override
		protected synchronized void runFinalizers() {
			super.runFinalizers();
		}
	}
}
