package org.gridkit.zerormi.io;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ByteStreamSink {	

	public boolean isActive();
	
	public void push(ByteBuffer data) throws IOException;
	
	/** 
	 * Indicates abnormal termination at side of data producer.
	 * Stream is inactive since this point.
	 */
	public void brokenStream(IOException error) throws ClosedStreamException;
	
	/** 
	 * Indicates graceful termination of data stream.
	 * Stream is inactive since this point.
	 */
	public void endOfStream() throws ClosedStreamException;
	
}
