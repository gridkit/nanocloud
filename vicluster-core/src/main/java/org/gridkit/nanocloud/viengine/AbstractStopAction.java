package org.gridkit.nanocloud.viengine;

import java.util.concurrent.ExecutionException;

public abstract class AbstractStopAction implements NodeAction {

    boolean runShutdown;
    boolean runPostShutdown;
    
    public AbstractStopAction(boolean runShutdown, boolean runPostShutdown) {
        this.runShutdown = runShutdown;
        this.runPostShutdown = runPostShutdown;
    }

    @Override
    public void run(PragmaWriter context) throws ExecutionException {
        if (runShutdown) {
            PragmaHelper.runHooks(context, Pragma.NODE_SHUTDOWN_HOOK);
            try {
                TextTerminalControl console = context.get(Pragma.RUNTIME_TEXT_TERMINAL);
                console.consoleFlush();
            }
            catch(Exception e) {
                // ignore
            }
        }
        stop();
        if (runPostShutdown) {
            PragmaHelper.runHooks(context, Pragma.NODE_POST_SHUTDOWN_HOOK);
        }
    }

    protected abstract void stop();
}
