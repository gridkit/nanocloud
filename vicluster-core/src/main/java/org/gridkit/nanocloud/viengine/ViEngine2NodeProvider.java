package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.viengine.NodeConfigHelper.action;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.require;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.NodeConfigurationException;
import org.gridkit.nanocloud.NodeExecutionException;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.AbstractCloudContext;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;

public class ViEngine2NodeProvider implements ViNodeProvider {

    public static final String LOG_STREAM__SHUTDOWN_EXCEPTON = "node.runtime.shutdown_exception";

    
    protected Context cloudContext;
    protected PragmaMap coreConfig = new PragmaMap();
	
    public ViEngine2NodeProvider() {
        cloudContext = new Context();
        initCoreConfig(coreConfig);
    }
    
	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		return true;
	}

	public ViEngine2NodeProvider addTypeInitializer(String type, String initClass) {
	    coreConfig.link(Pragma.BOOT_TYPE_INITIALIZER + "." + type, Pragma.INSTANTIATE + initClass);
	    return this;
	}

	public ViEngine2NodeProvider addTypeInitializer(String type, Class<?> initClass) {
	    coreConfig.link(Pragma.BOOT_TYPE_INITIALIZER + "." + type, Pragma.INSTANTIATE + initClass.getName());
	    return this;
	}
	
	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
	    PragmaMap map = coreConfig.clone();
	    map.set(Pragma.NODE_NAME, name);
	    Map<String, Object> nodeConfig = config.getConfigMap();
	    for(String key: nodeConfig.keySet()) {
	        if (key.indexOf(":") >= 0) {
	            map.set(key, nodeConfig.get(key));
	        }
	        else {
	            map.set(Pragma.PROP + key, (String)nodeConfig.get(key));	            
	        }
	    }
	    NodeBootstraper boostraper = new NodeBootstraper(map);
	    boostraper.boot();
	    ViEngine2 engine = new Engine(map);
	    return new ViEngine2Node(engine);
	}

	@Override
	public void shutdown() {
		cloudContext.runFinalizers();		
	}

	protected void initCoreConfig(PragmaMap config) {
		
	    DefaultNodeConfig.getDefaultConfig().copyTo(config);
	    
	    config.set(Pragma.NODE_CLOUD_CONTEXT, cloudContext);
	    
	    config.link(Pragma.BOOT_TYPE_INITIALIZER, Pragma.BOOT_TYPE_INITIALIZER + ".${vinode.type}");

	    config.link(Pragma.BOOT_SEQUENCE, Pragma.BOOT_SEQUENCE + ".${vinode.type}");

        // canonical boot sequence: configure init launch startup
        config.set(Pragma.DEFAULT + Pragma.BOOT_SEQUENCE + ".?*", "configure init launch startup");
        
        require(config, "launch", Pragma.RUNTIME_TEXT_TERMINAL, "TTY handler required");
        require(config, "launch", Pragma.RUNTIME_EXECUTOR, "Executor required");
        require(config, "launch", Pragma.RUNTIME_STOP_SWITCH);
        require(config, "launch", Pragma.RUNTIME_KILL_SWITCH);
        
        action(config, "startup", "process-pragmas", PragmaProcessor.INSTANCE);
	}

	private final class Context extends AbstractCloudContext {

		@Override
		protected synchronized void runFinalizers() {
			super.runFinalizers();
		}
	}

    class Engine implements ViEngine2 {

        PragmaWriter context;
        AdvancedExecutor executor;
        NodeAction stopSwitch;
        NodeAction killSwitch;
        boolean terminated;
        
        public Engine(PragmaWriter context) {
            this.context = context;
            executor = context.get(Pragma.RUNTIME_EXECUTOR);
            if (executor == null) {
                throw new NodeConfigurationException("No executor");
            }
            stopSwitch = context.get(Pragma.RUNTIME_STOP_SWITCH);
            if (stopSwitch == null) {
                throw new NodeConfigurationException("No stop switch");                
            }
            killSwitch = context.get(Pragma.RUNTIME_KILL_SWITCH);
            if (killSwitch == null) {
                throw new NodeConfigurationException("No kill switch");                
            }
        }
        
        @Override
        public AdvancedExecutor getExecutor() {
            return executor;
        }
    
        @Override
        public void shutdown() {
            try {
                stopSwitch.run(context);
            }
            catch(ExecutionException e) {
                PragmaHelper.log(context, LOG_STREAM__SHUTDOWN_EXCEPTON, "Node '%s' shutdown exception - %s", context.get(Pragma.NODE_NAME), e);
                throw new NodeExecutionException("Exception on shutdown", e.getCause());
            }
        }
    
        @Override
        public void kill() {
            try {
                killSwitch.run(context);
            }
            catch(ExecutionException e) {
                PragmaHelper.log(context, LOG_STREAM__SHUTDOWN_EXCEPTON, "Node '%s' shutdown exception - %s", context.get(Pragma.NODE_NAME), e);
                throw new NodeExecutionException("Exception on kill", e.getCause());
            }
        }
    
        @Override
        public Object getPragma(String key) {
            return PragmaHelper.getPragma(context, key);
        }
    
        @Override
        public void setPragmas(Map<String, Object> pragmas) {
            PragmaHelper.setPragmas(context, pragmas);
        }    
    }
}
