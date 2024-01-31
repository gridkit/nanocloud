package org.gridkit.nanocloud.viengine;

import java.util.Map;

import org.gridkit.zerormi.DirectRemoteExecutor;

public interface ViEngine2 {

    public DirectRemoteExecutor getExecutor();

    public void shutdown();

    public void kill();

    public Object getPragma(String key);

    public void setPragmas(Map<String, Object> pragmas);

}
