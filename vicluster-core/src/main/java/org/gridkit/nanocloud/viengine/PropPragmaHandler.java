package org.gridkit.nanocloud.viengine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.zerormi.DirectRemoteExecutor;

class PropPragmaHandler implements PragmaHandler {

    @Override
    public void configure(PragmaWriter conext) {
        // do nothing
    }

    @Override
    public void init(PragmaWriter conext) {
        // do nothing
    }

    @Override
    public Object query(PragmaWriter context, final String key) {
        DirectRemoteExecutor exec = context.get(Pragma.RUNTIME_EXECUTOR);
        final String propName = key.substring(Pragma.PROP.length());
        return AdvExecutor2ViExecutor.exec(exec, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return System.getProperty(propName);
            }
        });
    }

    @Override
    public void setup(PragmaWriter context, Map<String, Object> values) {
        apply(context, values);
    }

    @Override
    public void apply(PragmaWriter context, Map<String, Object> values) {
        final Map<String, String> props = new HashMap<String, String>();
        for(Map.Entry<String, Object> e: values.entrySet()) {
            props.put(e.getKey().substring(Pragma.PROP.length()), (String)e.getValue());
        }
        DirectRemoteExecutor exec = context.get(Pragma.RUNTIME_EXECUTOR);
        AdvExecutor2ViExecutor.exec(exec, new Runnable() {
            @Override
            public void run() {
                System.getProperties().putAll(props);
            }
        });
    }
}
