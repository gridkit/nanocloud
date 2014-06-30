package org.gridkit.nanocloud;

/**
 * Base class for Nanocloud exceptions.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NanocloudException extends RuntimeException {

	private static final long serialVersionUID = 20140118L;

	public NanocloudException() {
		super();
	}

	public NanocloudException(String message, Throwable cause) {
		super(message, cause);
	}

	public NanocloudException(String message) {
		super(message);
	}

	public NanocloudException(Throwable cause) {
		super(cause);
	}
}
