package org.gridkit.nanocloud.viengine;

import java.util.Map;

import org.gridkit.nanocloud.NodeConfigurationException;

class NotSupportedPragmaHandler implements PragmaHandler {

    private String message;
    
    public NotSupportedPragmaHandler(String message) {
        this.message = message;
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
    public void apply(PragmaWriter context, Map<String, Object> values) {
        String key = values.keySet().iterator().next();
        throw new NodeConfigurationException("Cannot apply '" + key + "'. " + message);
    }
}
