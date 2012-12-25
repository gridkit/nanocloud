package org.gridkit.zerormi.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.Latch;

public interface ByteStreamSource {

	public boolean isActive();
	
	/** 
	 * Optional. 
	 * Sets a {@link Latch} to be notified if data read in buffer.
	 * 
	 * Latch should never try to do a recursive calls to source.
	 * If stream line processing in required. Sink style connection
	 * should be used.
	 */
	public boolean setNotifier(Latch latch);
	
	/**
	 * @return amount of data available or -1 if EOF have been reached. If error is pending {@link #available()} would return positive value, even if there is no data in buffer.
	 * @throws IOException
	 */
	public int available();

	public void waitForData(int desiredSize);

	public void waitForData(int desiredSize, long timeout, TimeUnit tu);
		
	/**
	 * Do not forget to flip buffer. Before reading from it.
	 * @param buffer
	 * @throws IOException
	 */
	public void pull(ByteBuffer buffer) throws IOException;

	/**
	 * Notifies stream about consumer side problem. Dependent on implementation, exception cloud be pushed to producer.
	 * TODO
	 */
	public void brokenStream(IOException e) throws ClosedStreamException;
}
