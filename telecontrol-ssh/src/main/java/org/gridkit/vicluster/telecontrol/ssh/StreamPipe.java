package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class StreamPipe {
	
	private final byte[] buffer;
	private boolean closedByReader;
	private boolean closedByWriter;
	
	private int in = 0;
	private int out = 0;
	private int inBuffer = 0;
	
	public StreamPipe(int bufferSize) {
		buffer = new byte[bufferSize];
	}
	
	public InputStream getInputStream() {
		return new PipeIn();
	}
	
	public OutputStream getOutputStream() {
		return new PipeOut();
	}
	
	private int bufferRead(byte[] target, int offs, int size) throws IOException {
		int pending = waitForData();
		if (pending == 0) {
			// end of stream
			return -1;
		}
		int run = Math.min(size, pending);
		run = Math.min(run, buffer.length - in);
		System.arraycopy(buffer, in, target, offs, run);
		in = (in + run) % buffer.length;
		readNotify(run);
		return run;		
	}
	
	private void bufferWrite(byte[] data, int offs, int len) throws IOException {
		waitForBuffer(len);
		int run = buffer.length - out;
		if (run > len) {
			System.arraycopy(data, offs, buffer, out, len);
			out += len;
		}
		else {
			// wrap over buffer edge
			System.arraycopy(data, offs, buffer, out, run);
			System.arraycopy(data, offs + run, buffer, 0, len - run);
			out = len - run;			
		}
		writeNotify(len);
	}

	private synchronized void waitForBuffer(int size) throws IOException {
		while(true) {
			if (closedByReader) {
				throw new IOException("Pipe closed by reader");
			}
			else if (inBuffer + size < buffer.length) {
				return;
			}
			else {
				try {
					this.wait();
				} catch (InterruptedException e) {
					throw new IOException("Pipe write interrupted");
				}
			}
		}
	}
	
	private int waitForData() {
		if (inBuffer > 0) {
			return inBuffer;
		}
		synchronized(this) {
			while(true) {
				if (inBuffer > 0 || closedByWriter) {
					return inBuffer; 
				}
				else {
					try {
						this.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}
	
	private synchronized void writeNotify(int len) {
		inBuffer += len;
		this.notifyAll();		
	}

	private synchronized void readNotify(int len) {
		inBuffer -= len;
		this.notifyAll();		
	}

	private class PipeOut extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte)b});			
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			int s = off;
			int r = len;
			while(r > 0) {
				int p = Math.min(r, buffer.length / 2);
				bufferWrite(b, s, p);
				r -= p;
				s += p;
			}
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
			synchronized(StreamPipe.this) {
				closedByWriter = true;
				StreamPipe.this.notifyAll();
			}
		}
	}
	
	private class PipeIn extends InputStream {

		@Override
		public int read() throws IOException {
			byte[] bb = new byte[1];
			if (read(bb) < 0) {
				return -1;
			}
			else {
				return bb[0];
			}
		}

		@Override
		public int read(byte[] b) throws IOException {
			return bufferRead(b, 0, b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return bufferRead(b, off, len);
		}

		@Override
		public int available() throws IOException {
			return inBuffer;
		}

		@Override
		public void close() throws IOException {
			synchronized(StreamPipe.this) {
				closedByReader = true;
				StreamPipe.this.notifyAll();
			}
		}
	}
}
