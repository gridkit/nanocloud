package org.gridkit.nanocloud.viengine;

import java.util.Map;

import org.gridkit.util.concurrent.AdvancedExecutor;

public interface ViEngine2 {

    public AdvancedExecutor getExecutor();

    public void shutdown();

    public void kill();
    
    public Object getPragma(String key);
    
    public void setPragmas(Map<String, Object> pragmas);
    
}
