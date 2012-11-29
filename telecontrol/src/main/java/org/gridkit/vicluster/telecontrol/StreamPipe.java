/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.telecontrol;

import java.io.EOFException;
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
		if (size == 0) {
			if (closedByWriter && inBuffer == 0) {
				return -1;
			}
			else {
				return 0;
			}
		}
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
			if (closedByWriter) {
				throw new IOException("Pipe closed by writer");
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
	
	private int waitForData() throws IOException {
		if (inBuffer > 0) {
			return inBuffer;
		}
		synchronized(this) {
			while(true) {
				if (closedByReader) {
					throw new IOException("Pipe is closed by reader");
				}
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
	
	@Override
	public String toString() {
		return "Pipe@" + hashCode();
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

		@Override
		public String toString() {
			return "PipeOut@" + StreamPipe.this.hashCode();
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
				return (0XFF) & bb[0];
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
			int ib = inBuffer;
			if (ib > 0) {
				return ib;
			}
			else {
				synchronized(StreamPipe.this) {
					if (closedByReader) {
						throw new IOException("Pipe is closed by reader");
					}
					if (inBuffer == 0 && closedByWriter) {
						throw new EOFException("Pipe is closed by writer");
					}
				}
			}
			return inBuffer;
		}

		@Override
		public void close() throws IOException {
			synchronized(StreamPipe.this) {
				closedByReader = true;
				StreamPipe.this.notifyAll();
			}
		}
		
		@Override
		public String toString() {
			return "PipeIn@" + StreamPipe.this.hashCode();
		}
	}
}
