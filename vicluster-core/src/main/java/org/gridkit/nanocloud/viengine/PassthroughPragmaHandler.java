package org.gridkit.nanocloud.viengine;

import java.util.Map;

class PassthroughPragmaHandler implements PragmaHandler {

    @Override
    public void configure(PragmaWriter conext) {
        // do nothing
    }

    @Override
    public void init(PragmaWriter conext) {
        // do nothing
    }

    @Override
    public Object query(PragmaWriter context, String key) {
        return context.get(key);
    }

    @Override
    public void setup(PragmaWriter context, Map<String, Object> values) {
        apply(context, values);
    }

    @Override
    public void apply(PragmaWriter context, Map<String, Object> values) {
        for(String key: values.keySet()) {
            context.set(key, values.get(key));
        }
    }

}
