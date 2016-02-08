package org.gridkit.vicluster.telecontrol.jvm;

import java.util.Map;

import org.gridkit.nanocloud.telecontrol.ConsoleOutputSupport;
import org.gridkit.vicluster.AbstractCloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.zerormi.zlog.LogStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViEngineNodeProvider implements ViNodeProvider {

	protected Context context = new Context();
	
	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
	    ViEngine.Core core = null;
	    try {
	        Map<String, Object> engineConfig = config.getConfigMap();
    		engineConfig.put(ViConf.NODE_NAME, name);
    		engineConfig.put("vinode.name", name);
    		initConfigDefaults(engineConfig);
            core = new ViEngine.Core();
    		core.ignite(engineConfig);
    		ViEngineNode node = new ViEngineNode(core);
    		return node;
	    }
	    catch(Exception e) {
            if ("true".equalsIgnoreCase(String.valueOf(config.get(ViConf.NODE_DUMP_ON_FAILURE)))) {
                core.dumpCore(new WarnStream(LoggerFactory.getLogger(ViEngineNodeProvider.class)));
            }
	        throw Any.throwUnchecked(e);
	    }
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
		setDefault(engineConfig, ViConf.PRAGMA_HANDLER__REMOTE_RUNTIME,  new ViEngine.InitTimePragmaHandler());
		
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
	
    static class WarnStream implements LogStream {
        
        private Logger logger;
        
        public WarnStream(Logger logger) {
            this.logger = logger;
        }
        
        @Override
        public boolean isEnabled() {
            return logger.isWarnEnabled();
        }
        
        @Override
        public void log(String message) {
            logger.warn(message);            
        }
        
        @Override
        public void log(Throwable e) {
            logger.warn(e.toString(), e);            
        }

        @Override
        public void log(String name, Throwable e) {
            logger.warn(name, e);            
        }
        
        @Override
        public void log(String format, Object... argument) {
            logger.warn(format, argument);            
        }
    }	
}
