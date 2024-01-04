package org.gridkit.nanocloud;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConfigurable;

public class RemoteEx<X extends RemoteEx<?>> extends ViConfigurable.Delegate {

    public static String REMOTE_TARGET_URL = "remote:target-url";
    public static String JAR_CACHE_PATH = "remote:jar-cache-path";

    private ViConfigurable config;

    @SuppressWarnings("rawtypes")
    public static RemoteEx<?> at(ViConfigurable target) {
        return new RemoteEx(target);
    }

    protected RemoteEx(ViConfigurable target) {
        this.config = target;
    }

    @Override
    protected ViConfigurable getConfigurable() {
        return config;
    }

    @SuppressWarnings("unchecked")
    public X setRemoteNodeType() {
        config.setProp(ViConf.NODE_TYPE, ViConf.NODE_TYPE__REMOTE);
        // minimal setup for remoting
        setRemoteJarCachePath("/tmp/.nanocloud"); // TODO move to defaults
        return (X) this;
    }

    @SuppressWarnings("unchecked")
    public X setRemoteUrl(String url) {
        setConfigElement(RemoteEx.REMOTE_TARGET_URL, url);
        return (X) this;
    }

    @SuppressWarnings("unchecked")
    public X setRemoteJavaExec(String javaCmd) {
        setConfigElement(ViConf.JVM_EXEC_CMD, javaCmd);
        return (X) this;
    }

    @SuppressWarnings("unchecked")
    public X setRemoteJarCachePath(String jarCachePath) {
        setConfigElement(JAR_CACHE_PATH, jarCachePath);
        return (X) this;
    }

    @SuppressWarnings("unchecked")
    public X setTargetUrl(String url) {
        setConfigElement(REMOTE_TARGET_URL, url);
        return (X) this;
    }
}
