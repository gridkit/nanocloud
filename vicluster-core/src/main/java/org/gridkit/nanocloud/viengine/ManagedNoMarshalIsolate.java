package org.gridkit.nanocloud.viengine;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutorAdapter;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

public class ManagedNoMarshalIsolate implements ManagedProcess {

    private final Isolate isolate;
    private final ExecutorService exec;
    private final AdvancedExecutor aexec;

    public ManagedNoMarshalIsolate(Isolate isolate) {
        try {
            this.isolate = isolate;

            final FutureBox<ExecutorService> fexec = new FutureBox<ExecutorService>();
            isolate.exec(new Runnable() {

                @Override
                public void run() {
                    fexec.setData(Executors.newCachedThreadPool());
                }
            });
            exec = fexec.get();
            aexec = new AdvancedExecutorAdapter(exec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void suspend() {
        isolate.suspend();
    }

    @Override
    public void resume() {
        isolate.resume();
    }

    @Override
    public void destroy() {
        exec.shutdown();
        isolate.stop();
    }

    @Override
    public void consoleFlush() {
        // no action
    }

    @Override
    public FutureEx<Integer> getExitCodeFuture() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AdvancedExecutor getExecutionService() {
        return aexec;
    }

    @Override
    public void bindStdIn(InputStream is) {
        // do nothing
    }

    @Override
    public void bindStdOut(OutputStream os) {
        isolate.replaceSdtOut(asPrintStream(os));
    }

    @Override
    public void bindStdErr(OutputStream os) {
        isolate.replaceSdtErr(asPrintStream(os));
    }

    private PrintStream asPrintStream(OutputStream os) {
        if (os instanceof PrintStream) {
            return (PrintStream) os;
        } else {
            return new PrintStream(os);
        }
    }
}
