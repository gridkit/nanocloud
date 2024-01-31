package org.gridkit.nanocloud.viengine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViNodeExtender;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViNodeCore;
import org.gridkit.zerormi.DirectRemoteExecutor;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ViEngine2Node implements ViNodeCore {

    private ViEngine2 engine;

    public ViEngine2Node(ViEngine2 engine) {
        this.engine = engine;
    }

    @Override
    public <X> X x(ViNodeExtender<X> extention) {
        return extention.wrap(this);
    }

    @Override
    public <X> X x(ViConfExtender<X> extention) {
        return extention.wrap(this);
    }

    @Override
    public void touch() {
        // do nothing
    }

    @Override
    public DirectRemoteExecutor executor() {
        return engine.getExecutor();
    }

    @Override
    public void setProp(final String propName, final String value) {
        setProps(Collections.singletonMap(propName, value));
    }

    @Override
    public void setProps(Map<String, String> props) {
        for(String p: props.keySet()) {
            if (!ViConf.isVanilaProp(p)) {
                throw new IllegalArgumentException("[" + p + "] is not 'vanila' prop");
            }
        }
        Map<String, Object> pragmas = new HashMap<String, Object>();
        for(String key: props.keySet()) {
            pragmas.put(Pragma.PROP + key, props.get(key));
        }

        engine.setPragmas(pragmas);
    }

    @Override
    public void setConfigElement(String key, Object value) {
        engine.setPragmas(Collections.singletonMap(key, value));
    }

    @Override
    public void setConfigElements(Map<String, Object> config) {
        engine.setPragmas(config);
    }

    @Override
    public String getProp(final String propName) {
        return (String)engine.getPragma(Pragma.PROP + propName);
    }

    @Override
    public Object getPragma(final String pragmaName) {
        return engine.getPragma(pragmaName);
    }

    @Override
    public void kill() {
        engine.kill();
    }

    @Override
    public void shutdown() {
        engine.shutdown();
    }
}
