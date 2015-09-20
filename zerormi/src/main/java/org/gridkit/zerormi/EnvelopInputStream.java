package org.gridkit.zerormi;

import java.io.IOException;
import java.io.InputStream;

class EnvelopInputStream extends InputStream {

    private static boolean DEBUG = false;
    
    private InputStream source;
    private byte[] buffer = new byte[4 << 10];
    private int offs = 0;
    private int lim = 0;
    private boolean eom;
    
    public EnvelopInputStream(InputStream in) {
        this.source = in;
    }

    private int remaining() throws IOException {
        if (offs < lim) {
            return lim - offs;
        }
        else {
            if (eom) {
                return 0;
            }
            else {
                int chunk =  readShort();
                if (chunk == -1) {
                    eom = true;
                    return 0; // eof
                }

                if (chunk == 0) {
                    // end of message marker
                    if (DEBUG) {
                        System.out.println("[IN:" + hashCode() + "] - EOM");
                    }
                    eom = true;
                    return 0;
                }
                if (chunk < 0 || chunk > buffer.length) {
                    throw new IOException("Stream corrupted! Illegal chunk size " + chunk);
                }
                offs = 0;
                lim = chunk;
                readBuffer();
                return lim - offs;
            }
        }
    }
    
    int readShort() throws IOException {
        int ch1 = source.read();
        if (ch1 == -1) {
            return -1;
        }
        int ch2 = source.read();
        if ((ch1 | ch2) < 0) {
            throw new IOException("Stream trunkated");
        }
        return ((ch1 << 8) + (ch2 << 0));
    }
    
    private void readBuffer() throws IOException {
        int rp = 0;
        while(rp < lim) {
            int n = source.read(buffer, rp, lim - rp);
            if (n < 0) {
                throw new IOException("Stream truncated");
            }
            rp += n;
        }
        if (DEBUG) {
            System.out.println("[IN:" + hashCode() + "] - " + lim + "| " + IOHelper.toHexString(buffer, 0, lim));
        }
    }
    
    public void nextMessage() throws IOException {
        skip(Long.MAX_VALUE);
        eom = false;
    }
    
    @Override
    public int read() throws IOException {
        if (remaining() > 0) {
            return buffer[offs++];
        }
        else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {        
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining() == 0) {
            return -1;
        }
        int rlen = len;
        int roff = off;
        while(true) {
            int r = remaining();
            if (r == 0) {
                return len - rlen;
            }
            else if (r >= rlen) {
                System.arraycopy(buffer, offs, b, roff, rlen);
                offs += rlen;
                return len;
            }
            else {
                System.arraycopy(buffer, offs, b, roff, r);
                roff += r;
                rlen -= r;
                offs += r;
            }
        }
    }
    
    @Override
    public long skip(long n) throws IOException {
        long sr = n;
        while(true) {
            int r = remaining();
            if (r == 0) {
                return n - sr;
            }
            else if (r >= sr) {
                offs += sr;
                return 0;
            }
            else {
                sr -= r;
                offs = lim;
            }
        }
    }
}
