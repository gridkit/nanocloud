package org.gridkit.nanocloud.viengine;

import java.io.OutputStream;

public interface TextTerminalControl {

    /**
     * Pulls pending data from stdOut/stdErr pipes
     */
    public void consoleFlush();

    public OutputStream getStdIn();
    
    public void bindStdOut(OutputStream os);
    
    public void bindStdErr(OutputStream os);
    
}
