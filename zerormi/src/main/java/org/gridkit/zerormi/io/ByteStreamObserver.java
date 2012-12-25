package org.gridkit.zerormi.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.Latch;

public class ByteStreamObserver {

	private ByteStreamInputPin input; 
	private ByteStreamOutputPin output;
	
	private boolean sinkMode; 
	
	private long byteCounter = 0;
	private Exception error;
	private boolean eof = false;
	
	public ByteStreamInputPin getInputPin(boolean active) {
		boolean sinkMode = active;
		if (input == null) {
			initClutch(sinkMode);
		}
		if (this.sinkMode != sinkMode) {
			throw new IllegalArgumentException(sinkMode ? "Already in source-to-source mode" : "Already in sink-to-sink mode");
		}
		else {
			return input;
		}
	}
	
	public ByteStreamOutputPin getOutputPin(boolean active) {
		boolean sinkMode = !active;
		if (output == null) {
			initClutch(sinkMode);
		}
		if (this.sinkMode != sinkMode) {
			throw new IllegalArgumentException(sinkMode ? "Already in source-to-source mode" : "Already in sink-to-sink mode");
		}
		else {
			return output;
		}		
	}

	protected void initClutch(boolean sinkMode) {
		this.sinkMode = sinkMode;
		if (sinkMode) {
			SinkToSinkClutch clutch = new SinkToSinkClutch();
			input = clutch;
			output = new ByteStreamOutputPin.SinkWrapper(clutch);
		}
		else {
			SourceToSourceClutch clutch = new SourceToSourceClutch();
			output = clutch;
			input = new ByteStreamInputPin.SourceWrapper(clutch);
		}
	}
	
	public long getByteCount() {
		return byteCounter;
	}

	public boolean isClosed() {
		return eof;
	}

	public Exception getLastError() {
		return error;
	}
	
	private synchronized void notifyData(int intialPosition, ByteBuffer data) {
		byteCounter += data.position() - intialPosition;
	}

	private synchronized void notifyError(IOException e) {
		if (!eof) {
			error = e;
			eof = true;
		}
	}

	private synchronized void notifyEOF() {
		eof = true;
	}
	
	private class SinkToSinkClutch implements ByteStreamSink, ByteStreamInputPin {
		
		private ByteStreamSink sink;

		@Override
		public boolean isBound() {
			return sink != null;
		}

		@Override
		public boolean isActive() {
			return sink.isActive();
		}

		@Override
		public void push(ByteBuffer data) throws IOException {
			int ip = data.position();
			try {
				sink.push(data);
			}
			catch(IOException e) {
				notifyError(e);
				throw e;
			}
			notifyData(ip, data);			
		}

		@Override
		public void brokenStream(IOException error) throws ClosedStreamException {
			notifyError(error);
			sink.brokenStream(error);			
		}

		@Override
		public void endOfStream() throws ClosedStreamException {
			notifyEOF();
			sink.endOfStream();
		}

		@Override
		public boolean isActiveModeSupported() {
			return true;
		}

		@Override
		public boolean isPassiveModeSupported() {
			return false;
		}

		@Override
		public ByteStreamSource linkAsSource() {
			throw new IllegalArgumentException();
		}

		@Override
		public synchronized void linkToSink(ByteStreamSink sink) {
			if (sink == null) {
				throw new NullPointerException();
			}
			if (this.sink != null) {
				throw new IllegalArgumentException("Already bound");
			}
			this.sink = sink;
		}
	}
	
	private class SourceToSourceClutch implements ByteStreamSource, ByteStreamOutputPin {
		
		private ByteStreamSource source;

		@Override
		public boolean isActive() {
			return source.isActive();
		}

		@Override
		public boolean setNotifier(Latch latch) {
			return source.setNotifier(latch);
		}

		@Override
		public void waitForData(int desiredSize) {
			source.waitForData(desiredSize);
		}

		@Override
		public void waitForData(int desiredSize, long timeout, TimeUnit tu) {
			source.waitForData(desiredSize, timeout, tu);
		}

		@Override
		public int available() {
			int a = source.available();
			if (a == -1) {
				notifyEOF();
			}
			return a;
		}

		@Override
		public void pull(ByteBuffer buffer) throws IOException {
			int ip = buffer.position();
			try {
				source.pull(buffer);
			}
			catch(IOException e) {
				notifyError(e);
				throw e;
			}
			notifyData(ip, buffer);
		}

		@Override
		public void brokenStream(IOException e) throws ClosedStreamException {
			notifyError(e);
			source.brokenStream(e);			
		}

		@Override
		public boolean isBound() {
			return source != null;
		}

		@Override
		public boolean isActiveModeSupported() {
			return true;
		}

		@Override
		public boolean isPassiveModeSupported() {
			return false;
		}

		@Override
		public ByteStreamSink linkAsSink() {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void linkToSource(ByteStreamSource source) {
			if (source == null) {
				throw new NullPointerException();
			}			
			if (this.source != null) {
				throw new IllegalArgumentException("Already bound");
			}
			this.source = source;
		}
	}	
}
