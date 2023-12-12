package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.viengine.NodeConfigHelper.action;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.addPostPhase;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.addPrePhase;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.cloudSingleton;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.require;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.setDefault;
import static org.gridkit.nanocloud.viengine.NodeConfigHelper.setLazyDefault;

import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.telecontrol.ProcessSporeLauncher;
import org.gridkit.nanocloud.telecontrol.ZeroRmiRemoteSession;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.StreamCopyService;

public abstract class SlaveJvmNodeTypeInitializer implements NodeAction {

    protected static final LazyPragma SPORE_LAUNCHER = new LazyPragma() {

        @Override
        public Object resolve(String key, PragmaReader context) {
            StreamCopyService streamPump = context.get(ViConf.SPI_STREAM_COPY_SERVICE);
            return new ProcessSporeLauncher(streamPump);
        }

    };

    protected static final LazyPragma REMOTING_SESSION_FACTORY = new LazyPragma() {

        @Override
        public Object resolve(String key, PragmaReader context) {
            String nodeName = context.get(Pragma.NODE_NAME);
            return new ZeroRmiRemoteSession(nodeName);
        }
    };

    protected static final LaunchProcessAction LAUNCH_ACTION = new LaunchProcessAction();

    @Override
    public void run(PragmaWriter nodeConfig) throws ExecutionException {

        PragmaWriter config = getBaselineConfig();

        config.copyTo(nodeConfig, true);
    }

    private PragmaWriter getBaselineConfig() {

        PragmaWriter config = new PragmaMap();

        // Use default boot sequence

        configureDefaults(config);
        configurePragmas(config);
        configureCloudServices(config);
        configureClasspathSubphase(config);
        configureAgentSubphase(config);
        configureHostControlConsoleSubphase(config);
        configureDefaultJavaExec(config);
        configureRemoteSession(config);
        configureProcessLauncher(config);
        configureLaunchAction(config);

        return config;
    }

    protected void configureDefaults(PragmaWriter config) {
    }

    protected void configurePragmas(PragmaWriter config) {
        NodeConfigHelper.passivePragma(config, "jvm");
    }

    protected void configureCloudServices(PragmaWriter config) {
        cloudSingleton(config, ViConf.SPI_STREAM_COPY_SERVICE, BackgroundStreamDumper.StreamDumperService.class, "shutdown");
    }

    protected void configureLaunchAction(PragmaWriter config) {
        action(config, "launch", "launch-process", LAUNCH_ACTION);
    }

    protected void configureProcessLauncher(PragmaWriter config) {
        setLazyDefault(config, Pragma.RUNTIME_PROCESS_LAUNCHER, SPORE_LAUNCHER);
    }

    protected void configureRemoteSession(PragmaWriter config) {
        setLazyDefault(config, Pragma.RUNTIME_REMOTING_SESSION, REMOTING_SESSION_FACTORY);
    }

    protected void configureHostControlConsoleSubphase(PragmaWriter config) {
        addPrePhase(config, "launch", "console");
        require(config, "launch-console", Pragma.RUNTIME_HOST_CONTROL_CONSOLE, "Host control console required");
    }

    protected void configureClasspathSubphase(PragmaWriter config) {
        addPostPhase(config, "init", "classpath");
        require(config, "init-classpath", Pragma.RUNTIME_CLASSPATH, "Classpath configuration required");
        action(config, "init-classpath", "collect-classpath", ClasspathConfigurator.INSTANCE);
    }

    protected void configureAgentSubphase(PragmaWriter config) {
        addPostPhase(config, "init", "agents");
        require(config, "init-agents", Pragma.RUNTIME_AGENTS, "Agents configuration required");
        action(config, "init-agents", "collect-agents", AgentConfigurator.INSTANCE);
    }

    protected void configureDefaultJavaExec(PragmaWriter config) {
        setDefault(config, JvmConf.JVM_EXEC_CMD, "java");
    }

}
