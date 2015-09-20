package org.gridkit.zerormi;

import java.io.IOException;
import java.io.OutputStream;

public class EnvelopOutputStream extends OutputStream {

    private static boolean DEBUG = false;
    
    private OutputStream target;
    private byte[] buffer = new byte[4 << 10];
    private int offs = 0;
    
    public EnvelopOutputStream(OutputStream target) {
        this.target = target;
    }
    
    @Override
    public void write(int b) throws IOException {
        if (remaining() > 0) {
            buffer[offs++] = (byte) b;
        }
        
    }
    
    protected int remaining() throws IOException {
        int r = buffer.length - offs;
        if (r == 0) {
            flush();
            return remaining();
        }
        else {
            return r;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int roff = off;
        int rlen = len;
        while(rlen > 0) {
            int r = remaining();
            if (r >= rlen) {
                System.arraycopy(b, roff, buffer, offs, rlen);
                offs += rlen;
                return;
            }
            else {
                System.arraycopy(b, roff, buffer, offs, r);
                offs += r;
                roff += r;
                rlen -= r;
            }
        }
    }
    
    public void closeMessage() throws IOException {
        flush();
        writeShort(0);
        if (DEBUG) {
            System.out.println("[OUT:" + hashCode() + "] - EOM");
        }
    }
    
    @Override
    public void flush() throws IOException {
        if (offs > 0) {
            writeShort(offs);
            target.write(buffer, 0, offs);
            if (DEBUG) {
                System.out.println("[OUT:" + hashCode() + "] - " + offs + "| " + IOHelper.toHexString(buffer, 0, offs));
            }
            offs = 0;
        }
    }

    void writeShort(int v) throws IOException {
        target.write((v >>> 8) & 0xFF);
        target.write((v >>> 0) & 0xFF);
    }
    
    @Override
    public void close() throws IOException {
        flush();
        target.close();;
    }
}
