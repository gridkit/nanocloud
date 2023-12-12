package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.viengine.NodeConfigHelper.action;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.addPostPhase;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.cloudSingleton;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.require;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.setLazyDefault;

import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.telecontrol.ZeroRmiRemoteSession;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;

public class IsolateNodeTypeInitializer implements NodeAction {

    protected static final LazyPragma REMOTING_SESSION_FACTORY = new LazyPragma() {

        @Override
        public Object resolve(String key, PragmaReader context) {
            String nodeName = context.get(Pragma.NODE_NAME);
            return new ZeroRmiRemoteSession(nodeName);
        }
    };

    @Override
    public void run(PragmaWriter context) throws ExecutionException {

        PragmaWriter config = getBaselineConfig();
        config.copyTo(context, true);
    }

    private PragmaWriter getBaselineConfig() {

        PragmaWriter config = new PragmaMap();

        // Use default boot sequence

        config.link(IsolateProps.NAME, ViConf.NODE_NAME);

        configureCloudServices(config);
        configurePragmas(config);
        configureIsolateDefaults(config);
        configureClasspathSubphase(config);
        configureRemoteSession(config);
        configureLaunchAction(config);

        return config;
    }

    protected void configureCloudServices(PragmaWriter config) {
        cloudSingleton(config, ViConf.SPI_STREAM_COPY_SERVICE, BackgroundStreamDumper.StreamDumperService.class, "shutdown");
    }

    protected void configurePragmas(PragmaWriter config) {
        NodeConfigHelper.passivePragma(config, "isolate");
    }

    private void configureIsolateDefaults(PragmaWriter config) {
    }

    protected void configureLaunchAction(PragmaWriter config) {
        action(config, "launch", "launch-process", new LaunchIsolateAction());
    }

    protected void configureRemoteSession(PragmaWriter config) {
        setLazyDefault(config, Pragma.RUNTIME_REMOTING_SESSION, REMOTING_SESSION_FACTORY);
    }

    protected void configureClasspathSubphase(PragmaWriter config) {
        addPostPhase(config, "init", "classpath");
        require(config, "init-classpath", Pragma.RUNTIME_CLASSPATH, "Classpath configuration required");
        action(config, "init-classpath", "collect-classpath", ClasspathConfigurator.INSTANCE);
    }
}
