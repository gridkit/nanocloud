package org.gridkit.zerormi.io;

import java.io.IOException;

/**
 * Exception indicates that instance of stream have been closed. 
 */
public class ClosedStreamException extends IOException {

	private static final long serialVersionUID = 20121225L;

	public ClosedStreamException() {
		super();
	}

	public ClosedStreamException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClosedStreamException(String message) {
		super(message);
	}

	public ClosedStreamException(Throwable cause) {
		super(cause);
	}
}