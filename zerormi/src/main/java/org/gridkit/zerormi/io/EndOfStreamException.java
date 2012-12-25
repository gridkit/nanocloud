package org.gridkit.zerormi.io;

import java.io.EOFException;

/**
 * Exception indicates that, no more data in stream (for input streams).
 * It could also indicate that destination cannot accept more data. 
 */
public class EndOfStreamException extends EOFException {

	private static final long serialVersionUID = 20121225L;

	public EndOfStreamException() {
		super();
	}

	public EndOfStreamException(String s) {
		super(s);
	}		
}