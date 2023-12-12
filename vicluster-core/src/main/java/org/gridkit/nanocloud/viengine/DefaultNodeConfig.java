package org.gridkit.nanocloud.viengine;

import org.gridkit.vicluster.ViConf;

public class DefaultNodeConfig {

    private static final PragmaMap DEFAULT_CONFIG = new PragmaMap();

    private static final NodeLogStreamSupport LOG_STREAM_FACTORY = new NodeLogStreamSupport();
    private static final PropPragmaHandler PROP_PRAGMA_HANDLER = new PropPragmaHandler();
    private static final ConsolePragmaHandler CONSOLE_PRAGMA_HANDLER = new ConsolePragmaHandler();
    private static final RuntimePragmaHandler RUNTIME_PRAGMA_HANDLER = new RuntimePragmaHandler();


    static {
        init();
    }

    public static PragmaReader getDefaultConfig() {
        return DEFAULT_CONFIG;
    }

    private static void init() {

        // Special factories
        DEFAULT_CONFIG.setLazy(Pragma.DEFAULT + Pragma.INSTANTIATE + "**", LazyClassInstantiator.INSTANCE);
        DEFAULT_CONFIG.setLazy(Pragma.DEFAULT + Pragma.LOGGER_STREAM + "**", LOG_STREAM_FACTORY);

        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "node");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "boot");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "hook");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "node-runtime");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "default");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "lazy");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "new-instance");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "classpath");

        NodeConfigHelper.pragmaHandler(DEFAULT_CONFIG, "logger", LOG_STREAM_FACTORY);
        NodeConfigHelper.pragmaHandler(DEFAULT_CONFIG, "prop", PROP_PRAGMA_HANDLER);
        NodeConfigHelper.pragmaHandler(DEFAULT_CONFIG, "console", CONSOLE_PRAGMA_HANDLER);
        NodeConfigHelper.pragmaHandler(DEFAULT_CONFIG, "runtime", RUNTIME_PRAGMA_HANDLER);

        // backward compatibility hacks
        DEFAULT_CONFIG.link(Pragma.VINODE_NAME, Pragma.NODE_NAME);
        DEFAULT_CONFIG.link(Pragma.VINODE_TYPE, Pragma.NODE_TYPE);

        DEFAULT_CONFIG.link(Pragma.BOOT_TYPE_INITIALIZER, Pragma.BOOT_TYPE_INITIALIZER + ".${vinode.type}");

        DEFAULT_CONFIG.link(Pragma.BOOT_SEQUENCE, Pragma.BOOT_SEQUENCE + ".${vinode.type}");

        // canonical boot sequence: configure init launch startup
        DEFAULT_CONFIG.set(Pragma.DEFAULT + Pragma.BOOT_SEQUENCE + ".?*", "configure init launch startup");

        // console defaults
        NodeConfigHelper.setLazyDefault(DEFAULT_CONFIG, ViConf.CONSOLE_ECHO_PREFIX, EchoPrefix.INSTANCE);
        DEFAULT_CONFIG.set(ViConf.CONSOLE_STD_OUT_ECHO, "true");
        DEFAULT_CONFIG.set(ViConf.CONSOLE_STD_ERR_ECHO, "true");

        NodeConfigHelper.require(DEFAULT_CONFIG, "launch", Pragma.RUNTIME_MANAGED_PROCESS, "Managed process is required");
        NodeConfigHelper.require(DEFAULT_CONFIG, "launch", Pragma.RUNTIME_TEXT_TERMINAL, "TTY handler required");
        NodeConfigHelper.require(DEFAULT_CONFIG, "launch", Pragma.RUNTIME_EXECUTOR, "Executor required");
        NodeConfigHelper.require(DEFAULT_CONFIG, "launch", Pragma.RUNTIME_STOP_SWITCH);
        NodeConfigHelper.require(DEFAULT_CONFIG, "launch", Pragma.RUNTIME_KILL_SWITCH);

        NodeConfigHelper.action(DEFAULT_CONFIG, "init", "configure-pragmas", PragmaProcessor.CONFIGURE);
        NodeConfigHelper.action(DEFAULT_CONFIG, "startup", "process-pragmas", PragmaProcessor.INIT);
        NodeConfigHelper.action(DEFAULT_CONFIG, "startup", "process-hooks", StratupHookAction.INSTANCE);
    }

    private static class EchoPrefix implements LazyPragma {

        static EchoPrefix INSTANCE = new EchoPrefix();

        @Override
        public Object resolve(String key, PragmaReader context) {
            return "[" + context.get(Pragma.NODE_NAME) + "] ";
        }

    }
}
