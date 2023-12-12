package org.gridkit.nanocloud;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConfigurable;

public class RemoteEx extends ViConfigurable.Delegate {

    public static String REMOTE_TARGET_URL = "remote:target-url";
    public static String JAR_CACHE_PATH = "remote:jar-cache-path";

    private ViConfigurable config;

    public static RemoteEx at(ViConfigurable target) {
        return new RemoteEx(target);
    }

    protected RemoteEx(ViConfigurable target) {
        this.config = target;
    }

    @Override
    protected ViConfigurable getConfigurable() {
        return config;
    }

    public RemoteEx setRemoteNodeType() {
        config.setProp(ViConf.NODE_TYPE, ViConf.NODE_TYPE__REMOTE);
        // minimal setup for remoting
        setRemoteJarCachePath("/tmp/.nanocloud"); // TODO move to defaults
        return this;
    }

    public RemoteEx setRemoteJavaExec(String javaCmd) {
        setConfigElement(ViConf.JVM_EXEC_CMD, javaCmd);
        return this;
    }

    public RemoteEx setRemoteJarCachePath(String jarCachePath) {
        config.setProp(JAR_CACHE_PATH, jarCachePath);
        return this;
    }

    public RemoteEx setTargetUrl(String url) {
        config.setProp(REMOTE_TARGET_URL, url);
        return this;
    }
}
