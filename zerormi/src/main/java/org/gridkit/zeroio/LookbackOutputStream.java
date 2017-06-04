package org.gridkit.zeroio;

import java.io.IOException;
import java.io.OutputStream;

public class LookbackOutputStream extends OutputStream {

	private OutputStream sink;
	private byte[] buffer;
	private int start;
	private int end;
	private long byteCounter;
	private volatile boolean closed;
	private volatile boolean clean = true;
	
	public LookbackOutputStream(int bufferSize) {
		buffer = new byte[bufferSize];
	}
	
	public synchronized byte[] getLookbackBuffer() {
		byte[] bb = new byte[(int)Math.min(buffer.length, byteCounter)];
		for(int i = 0; i != bb.length; ++i) {
			bb[bb.length - i - 1] = buffer[(buffer.length + end - i - 1) % buffer.length];
		}
		return bb;
	}

	public synchronized long getWrittenBytes() {
		return byteCounter;
	}

	public synchronized OutputStream getOutput() {
		return sink;
	}
	
	public synchronized boolean isClosed() {
		return closed;
	}

	public synchronized void setOutput(OutputStream os) throws IOException {
		sink = os;
		pump();
		if (closed) {
			sink.close();
		}
	}
	
	protected int size() {
		return (buffer.length + end - start) % buffer.length; 
	}
	
	@Override
	public synchronized void write(int b) throws IOException {
		if (size() > buffer.length / 2) {
			pump();
		}
		buffer[end] = (byte)b;
		end = (end + 1) % buffer.length;
		if (end == start) {
			start = (start + 1) % buffer.length;
		}
		++byteCounter;
		clean = false;
	}

	private synchronized void pump() throws IOException {
		if (sink != null && size() > 0) {
			if (end >= start) {
				sink.write(buffer, start, end - start);
				start = end;
			}
			else {
				sink.write(buffer, start, buffer.length - start);
				sink.write(buffer, 0, end);
				start = end;
			}
		}
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		// TODO more efficient coping
		for(int i = 0; i != len; ++i) {//new String(b, off, len)
			write(b[off + i]);
		}
		pump();
	}

	@Override
	public void flush() throws IOException {
	    if (clean) {
	        // deadlock protection
	        return;
	    }
	    synchronized(this) {	        
    		if (sink != null) {
    		    clean = true;
    			pump();
    			sink.flush();
    		}
	    }
	}

	@Override
	public void close() throws IOException {
	    if (closed) {
	        return;
	    }
	    synchronized(this) {
    		if (sink != null) {
    			closed = true;
    			flush();
    			sink.close();
    		}
	    }
	}
}
