package org.gridkit.zerormi.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BytePipeTestSupport implements ByteSourceTestSupport, ByteSinkTestSupport {
	
	private final ByteStreamPipe pipe;
	private final ByteStreamSink sink;
	private final ByteStreamSource source;
	
	public BytePipeTestSupport(int size) {
		this.pipe = new ByteStreamPipe(size);
		this.sink = pipe.getOutput().linkAsSink();
		this.source = pipe.getInput().linkAsSource();
	}
	
	@Override
	public ByteStreamSink getSink() {
		return sink;
	}
	@Override
	public byte[] read() throws IOException {
		int n = source.available();
		if (n < 0) {
			throw new EOFException();
		}
		if (n == 0) {
			return new byte[0];
		}
		ByteBuffer buf = ByteBuffer.allocate(n);
		while(buf.remaining() > 0) {
			source.pull(buf);
		}
		// error prone
		return buf.array();
	}
	
	@Override
	public String readString() throws IOException {
		return new String(read());
	}
	
	@Override
	public boolean isEOF() {
		return source.available() < 0;
	}
	
	@Override
	public void pushConsumerException(IOException e) throws ClosedStreamException {
		source.brokenStream(e);
	}

	@Override
	public ByteStreamSource getSource() {
		return source;
	}

	@Override
	public void write(byte[] data) throws IOException {
		ByteBuffer wrap = ByteBuffer.wrap(data);
		sink.push(wrap);		
	}

	@Override
	public void write(String data) throws IOException {
		write(data.getBytes());		
	}

	@Override
	public void pushProducerException(IOException e) throws ClosedStreamException {
		sink.brokenStream(e);		
	}

	@Override
	public void closeOutput() throws ClosedStreamException {
		sink.endOfStream();
	}

	@Override
	public void flusOutput() throws ClosedStreamException {
		// nothing to do
	}
}
