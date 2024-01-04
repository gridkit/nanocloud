package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.viengine.NodeConfigHelper.action;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.addPrePhase;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.cloudSingleton;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.setDefault;

import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.RemoteEx;
import org.gridkit.vicluster.ViConf;

class RemoteNodeTypeInitializer extends SlaveJvmNodeTypeInitializer {

    private static final NodeAction DISABLE_SHALLOW_CLASSPATH = new NodeAction() {

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            context.set(ViConf.CLASSPATH_USE_SHALLOW, "false");
        }
    };

    @Override
    protected void configureDefaults(PragmaWriter config) {
        setDefault(config, RemoteEx.JAR_CACHE_PATH, "/tmp/.nanocloud");
        // shallow classpath is no supported
        config.set(ViConf.CLASSPATH_USE_SHALLOW, "false");
    }

    @Override
    protected void configurePragmas(PragmaWriter config) {
        super.configurePragmas(config);
        NodeConfigHelper.passivePragma(config, "remote");
    }

    @Override
    protected void configureHostControlConsoleSubphase(PragmaWriter config) {
        cloudSingleton(config, Pragma.RUNTIME_REMOTE_CONNECTION_MANAGE, RemoteControlConnectionManager.class, "terminate");

        NodeConfigHelper.passivePragma(config, "remote-protocol");
        NodeConfigHelper.passivePragma(config, "remote-protocol-connector");

        config.set(Pragma.REMOTE_PROTOCOL + "_", EndPointConnectorAction.INSTANCE);

        config.set(Pragma.REMOTE_PROTOCOL_CONNECTOR + "tcp", PlainTpcSocketConnectorFactory.INSTANCE);

        action(config, "configure", "DisableShallowClasspath", DISABLE_SHALLOW_CLASSPATH);

        addPrePhase(config, "launch", "connect");
        // TODO collect URI connectors from config
        action(config, "launch-connect", "connect", InitConnectorAction.INSTANCE);

        super.configureHostControlConsoleSubphase(config);

        addPrePhase(config, "launch", "console");
        action(config, "launch-console", "open-console", TunnlerInitAction.INSTANCE);

        action(config, "launch-console", "open-console", TunnlerInitAction.INSTANCE);
    }

}
