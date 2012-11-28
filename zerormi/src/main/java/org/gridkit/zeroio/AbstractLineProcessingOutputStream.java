package org.gridkit.zeroio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Alexey Ragozin
 */
abstract class AbstractLineProcessingOutputStream extends OutputStream {

	private ByteArrayOutputStream buffer;
	
	public AbstractLineProcessingOutputStream() {
		this.buffer = new ByteArrayOutputStream();
	}
	
	private void dumpBuffer() throws IOException {
		processLine(buffer.toByteArray());
		buffer.reset();
	}
	
	protected abstract void processLine(byte[] data) throws IOException;
	
	@Override
	public synchronized void write(int c) throws IOException {
		buffer.write(c);
		if (c == '\n') {
			dumpBuffer();
		}
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		for (int i = 0; i != len; ++i) {
			if (b[off + i] == '\n') {
				writeByChars(b, off, len);
				return;
			}
		}
		buffer.write(b, off, len);
	}

	private void writeByChars(byte[] cbuf, int off, int len) throws IOException {
		for (int i = 0; i != len; ++i) {
			write(cbuf[off + i]);
		}
	}

	@Override
	public synchronized void close() throws IOException {
		super.flush();
		dumpBuffer();			
	}
}