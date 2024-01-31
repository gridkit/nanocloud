package org.gridkit.nanocloud.viengine;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.NodeConfigurationException;
import org.gridkit.nanocloud.NodeExecutionException;
import org.gridkit.vicluster.AbstractCloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeCore;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.zerormi.DirectRemoteExecutor;

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
    public ViNodeCore createNode(String name, ViNodeConfig config) {
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

    }

    private final class Context extends AbstractCloudContext {

        @Override
        protected synchronized void runFinalizers() {
            super.runFinalizers();
        }
    }

    class Engine implements ViEngine2 {

        PragmaWriter context;
        DirectRemoteExecutor executor;
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
        public DirectRemoteExecutor getExecutor() {
            return executor;
        }

        @Override
        public void shutdown() {
            try {
                stopSwitch.run(context);
                PragmaHelper.runHooks(context, Pragma.NODE_FINALIZER);
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
                PragmaHelper.runHooks(context, Pragma.NODE_FINALIZER);
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
            PragmaHelper.setPragmas(context, false, pragmas);
        }
    }

    public void initDefaultProviders() {
        // TODO in process support
        addTypeInitializer(ViConf.NODE_TYPE__IN_PROCESS, IsolateNodeTypeInitializer.class);
        addTypeInitializer(ViConf.NODE_TYPE__ISOLATE, IsolateNodeTypeInitializer.class);
        addTypeInitializer(ViConf.NODE_TYPE__LOCAL, LocalNodeTypeInitializer.class);
        addTypeInitializer(ViConf.NODE_TYPE__REMOTE, RemoteNodeTypeInitializer.class);
    }
}
