package org.gridkit.zerormi.io;

import java.io.IOException;

public interface ByteSinkTestSupport {

	public ByteStreamSink getSink();
	
	public byte[] read() throws IOException;

	public String readString() throws IOException;
	
	public boolean isEOF();
	
	public void pushConsumerException(IOException e) throws ClosedStreamException;
	
}
