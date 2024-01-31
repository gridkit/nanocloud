package org.gridkit.vicluster.telecontrol;

import java.io.InputStream;
import java.io.OutputStream;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.DirectRemoteExecutor;

public interface ManagedProcess {

    public void suspend();

    public void resume();

    public void destroy();

    /**
     * Pulls pending data from stdOut/stdErr pipes
     */
    public void consoleFlush();

    public FutureEx<Integer> getExitCodeFuture();

    public DirectRemoteExecutor getExecutionService();

    public void bindStdIn(InputStream is);

    public void bindStdOut(OutputStream os);

    public void bindStdErr(OutputStream os);

}
