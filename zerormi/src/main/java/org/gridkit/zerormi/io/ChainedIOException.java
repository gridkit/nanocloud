package org.gridkit.zerormi.io;

import java.io.IOException;

public class ChainedIOException extends IOException {
	
	private static final long serialVersionUID = 20121225L;

	public ChainedIOException(IOException e) {
		super(e);
	}
}
