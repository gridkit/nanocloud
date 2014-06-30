package org.gridkit.nanocloud;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NodeStartupRuntimeException extends NanocloudException {

	private static final long serialVersionUID = 20140118L;

	public NodeStartupRuntimeException() {
		super();
	}

	public NodeStartupRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public NodeStartupRuntimeException(String message) {
		super(message);
	}

	public NodeStartupRuntimeException(Throwable cause) {
		super(cause);
	}
}
