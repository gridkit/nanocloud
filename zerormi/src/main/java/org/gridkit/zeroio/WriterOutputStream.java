package org.gridkit.zeroio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public class WriterOutputStream extends OutputStream {

	private final Writer writer;
	
	public WriterOutputStream(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void write(int b) throws IOException {
		writer.append((char)b);
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
