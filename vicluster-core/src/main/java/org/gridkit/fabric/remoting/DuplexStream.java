package org.gridkit.fabric.remoting;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DuplexStream extends Closeable {
	
	public InputStream getInput() throws IOException;
	
	public OutputStream getOutput() throws IOException;
	
	public boolean isClosed();
	
	public void close() throws IOException;
	
	public String toString();

}
