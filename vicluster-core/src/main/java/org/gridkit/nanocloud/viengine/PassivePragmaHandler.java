package org.gridkit.nanocloud.viengine;

import java.util.Map;

class PassivePragmaHandler implements PragmaHandler {

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
        String phase = context.get(Pragma.BOOT_PHASE);
        if (phase != null && phase.length() == 0) {
            throw new IllegalStateException("Node already started");
        }
        else {
            // ignore
        }
    }
}
