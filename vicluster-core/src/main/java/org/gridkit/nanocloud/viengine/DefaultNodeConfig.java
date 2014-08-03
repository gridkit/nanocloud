package org.gridkit.nanocloud.viengine;


public class DefaultNodeConfig {

    private static final PragmaMap DEFAULT_CONFIG = new PragmaMap();
    
    private static final NodeLogStreamSupport LOG_STREAM_FACTORY = new NodeLogStreamSupport();
    private static final PropPragmaHandler PROP_PRAGMA_HANDLER = new PropPragmaHandler();
    
    
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
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "node-runtime");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "default");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "lazy");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "new-instance");
        NodeConfigHelper.passivePragma(DEFAULT_CONFIG, "classpath");

        NodeConfigHelper.pragmaHandler(DEFAULT_CONFIG, "logger", LOG_STREAM_FACTORY);
        NodeConfigHelper.pragmaHandler(DEFAULT_CONFIG, "prop", PROP_PRAGMA_HANDLER);

        
        // backward compatibility hacks
        DEFAULT_CONFIG.link(Pragma.VINODE_NAME, Pragma.NODE_NAME);
        DEFAULT_CONFIG.link(Pragma.VINODE_TYPE, Pragma.NODE_TYPE);
        
    }
    
}
