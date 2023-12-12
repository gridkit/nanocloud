package org.gridkit.nanocloud.interceptor;

import org.gridkit.nanocloud.viengine.NodeTrigger;
import org.gridkit.nanocloud.viengine.Pragma;
import org.gridkit.nanocloud.viengine.PragmaWriter;
import org.gridkit.util.concurrent.AdvancedExecutor;

class IsolateInitializer implements NodeTrigger {

    public static final IsolateInitializer INSTANCE = new IsolateInitializer();

    @Override
    public KeyMatcher keyMatcher() {
        return new SinglePropMatcher(Pragma.RUNTIME_EXECUTOR);
    }

    @Override
    public boolean evaluate(PragmaWriter context) {
        AdvancedExecutor executor = context.get(Pragma.RUNTIME_EXECUTOR);
        if (executor != null) {
            InstrumentationInitializer.configureIsolate(executor);
            return true;
        } else {
            return false;
        }
    }
}
