package org.gridkit.zerormi.io;

import java.io.IOException;

public interface ByteSourceTestSupport {

	public ByteStreamSource getSource();
	
	public void write(byte[] data) throws IOException;

	public void write(String data) throws IOException;
	
	public void pushProducerException(IOException e) throws ClosedStreamException;
	
	public void closeOutput() throws ClosedStreamException;
	
}
