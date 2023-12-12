package org.gridkit.nanocloud.viengine;

import java.util.concurrent.ExecutionException;

class StratupHookAction implements NodeAction {

    public static final StratupHookAction INSTANCE = new StratupHookAction();

    @Override
    public void run(PragmaWriter context) throws ExecutionException {
        PragmaHelper.runHooks(context, Pragma.NODE_STARTUP_HOOK);
    }
}
