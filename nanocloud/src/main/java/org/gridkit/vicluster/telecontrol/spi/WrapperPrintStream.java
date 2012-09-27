package org.gridkit.vicluster.telecontrol.spi;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class WrapperPrintStream extends FilterOutputStream {

	private String prefix;
	private PrintStream printStream;
	private ByteArrayOutputStream buffer;
	
	public WrapperPrintStream(String prefix, PrintStream printStream) {
		super(printStream);
		this.prefix = prefix;
		this.printStream = printStream;
		this.buffer = new ByteArrayOutputStream();
	}
	
	private void dumpBuffer() throws IOException {
		printStream.append(prefix);
		printStream.write(buffer.toByteArray());
		printStream.flush();
		buffer.reset();
	}
	
	@Override
	public synchronized void write(int c) throws IOException {
		synchronized(printStream) {
			buffer.write(c);
			if (c == '\n') {
				dumpBuffer();
			}
		}
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		synchronized(printStream) {
			for (int i = 0; i != len; ++i) {
				if (b[off + i] == '\n') {
					writeByChars(b, off, len);
					return;
				}
			}
			buffer.write(b, off, len);
		}
	}

	private void writeByChars(byte[] cbuf, int off, int len) throws IOException {
		for (int i = 0; i != len; ++i) {
			write(cbuf[off + i]);
		}
	}

	@Override
	public void close() throws IOException {
		super.flush();
		dumpBuffer();			
	}
}	
