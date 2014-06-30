package org.gridkit.nanocloud.viengine;

import java.io.IOException;
import java.io.OutputStream;

class NullConsole implements TextTerminalControl {

    private static OutputStream NULL_STREAM = new OutputStream() {
        
        @Override
        public void write(int b) throws IOException {
            // do nothing;            
        }

        @Override
        public void write(byte[] b) throws IOException {
            // do nothing;            
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // do nothing;            
        }

        @Override
        public void flush() throws IOException {
            // do nothing;            
        }

        @Override
        public void close() throws IOException {
            // do nothing;            
        }
    };
    
    @Override
    public void consoleFlush() {
        // do nothing        
    }

    @Override
    public OutputStream getStdIn() {
        return NULL_STREAM;
    }

    @Override
    public void bindStdOut(OutputStream os) {
        // do nothing
    }

    @Override
    public void bindStdErr(OutputStream os) {
        // do nothing
    }
}
