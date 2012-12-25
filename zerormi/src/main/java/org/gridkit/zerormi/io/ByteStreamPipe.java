package org.gridkit.zerormi.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.Latch;

public class ByteStreamPipe implements ByteStreamPinPair {
	
	private static long ANCHOR = System.nanoTime();
	
	private static long now() {
		return System.nanoTime() - ANCHOR;
	}
	
	private final byte[] buffer;
	private boolean closedByReader;
	private boolean closedByWriter;
	private IOException writePushError;
	private IOException readPushError;
	
	private ByteStreamInputPin input = new ByteStreamInputPin.SourceWrapper(new PipeIn());
	private ByteStreamOutputPin output = new ByteStreamOutputPin.SinkWrapper(new PipeOut());
	
	private int in = 0;
	private int out = 0;
	private int inBuffer = 0;
	private Latch readerLatch;
	
	public ByteStreamPipe(int bufferSize) {
		if (bufferSize < 4) {
			throw new IllegalArgumentException("Buffer should be at least 4 bytes");
		}
		buffer = new byte[bufferSize];
	}

	@Override
	public ByteStreamInputPin getInput() {
		return input;
	}

	@Override
	public ByteStreamOutputPin getOutput() {
		return output;
	}

	// TODO use buffer directly
	private void bufferRead(ByteBuffer buffer) throws IOException {
		if (!buffer.isDirect() && !buffer.isReadOnly()) {
			int n = bufferRead(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
			buffer.position(buffer.position() + n);
		}
		else {
			byte[] buf = new byte[buffer.remaining()];
			int n = bufferRead(buf, 0, buf.length);
			if (n > 0) {
				buffer.put(buf, 0, n);
			}
			else {
				throw new EOFException();
			}
		}
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
		int pending = waitForData(1, 1000, TimeUnit.DAYS);
		if (pending == 0) {
			if (writePushError != null) {
				throw new ChainedIOException(writePushError);
			}
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
	
	// TODO use buffer directly
	private void bufferWrite(ByteBuffer data) throws IOException {
		while(data.remaining() > 0) {
			int avail = waitForBuffer(4);
			if (avail > data.remaining()) {
				avail = data.remaining();
			}
			int run = buffer.length - out;
			if (run >= avail) {
				data.get(buffer, out, avail);
				out += avail;
			}
			else {
				// wrap over buffer edge
				if (run > 0) {
					data.get(buffer, out, run);
				}
				data.get(buffer, 0, avail - run);
				out = avail - run;			
			}
			writeNotify(avail);
		}
	}
	
	@SuppressWarnings("unused")
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

	private synchronized int waitForBuffer(int size) throws IOException {
		
		while(true) {
			if (closedByReader) {
				if (readPushError != null) {
					throw new ChainedIOException(readPushError);
				}
				else {
					throw new IOException("Pipe closed by reader");
				}
			}
			if (closedByWriter) {
				throw new ClosedStreamException("Pipe closed by writer");
			}
			else if (inBuffer + size < buffer.length) {
				return buffer.length - inBuffer;
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
	
	private int waitForData(int size, long timeout, TimeUnit tu) throws IOException {
		if (size <= 0) {
			throw new IllegalArgumentException("Negative size");
		}
		long deadline = now() + tu.toNanos(timeout);
		if (inBuffer >= size) {
			return inBuffer;
		}
		synchronized(this) {
			while(true) {
				if (closedByReader) {
					throw new ClosedStreamException("Pipe is closed by reader");
				}
				if (inBuffer > 0 || closedByWriter) {
					return inBuffer; 
				}
				else {
					try {
						long wait = TimeUnit.NANOSECONDS.toMillis(deadline - now());
						if (wait > 1000) {
							wait = 1000;
						}
						if (wait > 0) {
							this.wait(wait);
						}
						if (deadline <= now()) {
							return inBuffer;
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}
	
	private void writeNotify(int len) {
		synchronized(this) {
			inBuffer += len;
			this.notifyAll();
		}
		if (readerLatch != null) {
			readerLatch.open();
		}
	}

	private synchronized void readNotify(int len) {
		inBuffer -= len;
		this.notifyAll();		
	}
	
	private void readerCloseNotify() {
		synchronized (this) {
			closedByReader = true;
			this.notifyAll();		
		}
		if (readerLatch != null) {
			readerLatch.notify();			
		}
	}

	private void writerCloseNotify() {
		synchronized (this) {
			closedByWriter = true;
			this.notifyAll();		
		}
		if (readerLatch != null) {
			readerLatch.notify();			
		}
	}
	
	@Override
	public String toString() {
		return "Pipe@" + hashCode();
	}

	private class PipeOut implements ByteStreamSink {

		volatile boolean active = true;
		
		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public synchronized void push(ByteBuffer data) throws IOException {
			if (!active) {
				throw new ClosedStreamException();
			}
			try {
				bufferWrite(data);
			}
			catch(IOException e) {
				active = false;
				throw e;
			}
		}

		@Override
		public synchronized void brokenStream(IOException error) throws ClosedStreamException {
			if (!active) {
				throw new ClosedStreamException();
			}
			active = false;
			writePushError = error;
			writerCloseNotify();
			if (error == null) {
				throw new NullPointerException();
			}
		}

		@Override
		public synchronized void endOfStream() throws ClosedStreamException {
			if (!active) {
				throw new ClosedStreamException();
			}
			active = false;
			writerCloseNotify();			
		}

		@Override
		public String toString() {
			return "PipeOut@" + ByteStreamPipe.this.hashCode();
		}
	}
	
	private class PipeIn implements ByteStreamSource {

		volatile boolean active = true;
		
		@Override
		public boolean isActive() {
			return active;
		}
		
		@Override
		public boolean setNotifier(Latch latch) {
			if (readerLatch != null) {
				throw new IllegalStateException("Latch is already set");
			}
			readerLatch = latch;
			return true;
		}

		@Override
		public int available() {
			int a = checkAvailable();
			if (a < 0) {
				active = false;
			}
			return a;
		}
		
		private int checkAvailable() {
			int ib = inBuffer;
			if (ib > 0) {
				return ib;
			}
			else {
				synchronized(ByteStreamPipe.this) {
					if (closedByReader) {
						return -1;
					}
					if (inBuffer == 0 && closedByWriter) {
						if (active && writePushError != null) {
							return 1;
						}
						else {
							return -1;
						}
					}
				}
			}
			return inBuffer;
		}

		@Override
		public void waitForData(int desiredSize) {
			waitForData(desiredSize, 1000, TimeUnit.DAYS);
		}

		@Override
		public void waitForData(int desiredSize, long timeout, TimeUnit tu) {
			try {
				ByteStreamPipe.this.waitForData(desiredSize, timeout, tu);
			} catch (IOException e) {
				// ignore exception, will be thrown again on data access
			}			
		}

		@Override
		public void pull(ByteBuffer buffer) throws IOException {
			if (!active) {
				throw new ClosedStreamException();
			}
			try {
				bufferRead(buffer);
			}
			catch(IOException e) {
				active = false;
			}
		}

		@Override
		public synchronized void brokenStream(IOException e) throws ClosedStreamException {
			if (!active) {
				throw new ClosedStreamException();
			}
			active = false;
			readPushError = e;
			readerCloseNotify();
			if (e == null) {
				throw new NullPointerException();
			}
		}

		@Override
		public String toString() {
			return "PipeIn@" + ByteStreamPipe.this.hashCode();
		}
	}
}
