package org.gridkit.nanocloud.viengine;

import java.io.OutputStream;

import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamPipe;

public class ManagedProcessTextTerminal implements TextTerminalControl {

    private ManagedProcess process;
    private StreamPipe stdInPipe = new StreamPipe(1 << 10);
    private OutputStream processIn;
    
    
    
    public ManagedProcessTextTerminal(ManagedProcess process) {
        this.process = process;
        this.process.bindStdIn(stdInPipe.getInputStream());
        this.processIn = stdInPipe.getOutputStream();        
    }

    @Override
    public void consoleFlush() {
        process.consoleFlush();
    }

    @Override
    public OutputStream getStdIn() {
        return processIn;
    }

    @Override
    public void bindStdOut(OutputStream os) {
        process.bindStdOut(os);
    }

    @Override
    public void bindStdErr(OutputStream os) {
        process.bindStdErr(os);
    }
}
