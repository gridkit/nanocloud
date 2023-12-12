package org.gridkit.nanocloud.telecontrol.isolate;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViConfigurable.Delegate;
import org.gridkit.vicluster.isolate.IsolateProps;

public class IsolateConfig extends Delegate {

    /**
     * Disable marshaling with isolate communication. Useful is no classloader isolation is used.
     */
    public static String NO_MARSHAL = "isolate:no-marshal";

    /**
     * Share all classes.
     */
    public static String SHARE_ALL_CLASSES = "isolate:share-all";

    private ViConfigurable conf;

    public static IsolateConfig at(ViConfigurable conf) {
        return new IsolateConfig(conf);
    }

    public IsolateConfig(ViConfigurable conf) {
        this.conf = conf;
    }

    @Override
    protected ViConfigurable getConfigurable() {
        return conf;
    }

    public IsolateConfig setIsolateNodeType() {
        conf.setProp(ViConf.NODE_TYPE, ViConf.NODE_TYPE__ISOLATE);
        return this;
    }

    public IsolateConfig shareClass(Class<?> c) {
        shareClass(c.getName());
        return this;
    }

    public IsolateConfig shareClass(String name) {
        conf.setProp(IsolateProps.SHARE_CLASS + name, name);
        return this;
    }

    public IsolateConfig sharePackage(String name) {
        conf.setProp(IsolateProps.SHARE_PACKAGE + name, name);
        return this;
    }

    public IsolateConfig isolateClass(Class<?> c) {
        isolateClass(c.getName());
        return this;
    }

    public IsolateConfig isolateClass(String name) {
        conf.setProp(IsolateProps.ISOLATE_CLASS + name, name);
        return this;
    }

    public IsolateConfig isolatePackage(String name) {
        conf.setProp(IsolateProps.ISOLATE_PACKAGE + name, name);
        return this;
    }

    public IsolateConfig noMarshal(boolean enabled) {
        conf.setProp(IsolateConfig.NO_MARSHAL, String.valueOf(enabled));
        return this;
    }

    public IsolateConfig shareAllClasses(boolean enabled) {
        conf.setProp(IsolateConfig.SHARE_ALL_CLASSES, String.valueOf(enabled));
        return this;
    }
}
