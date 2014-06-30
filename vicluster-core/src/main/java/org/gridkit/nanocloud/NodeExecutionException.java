package org.gridkit.nanocloud;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NodeExecutionException extends NanocloudException {

	private static final long serialVersionUID = 20140118L;

	public NodeExecutionException() {
		super();
	}

	public NodeExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public NodeExecutionException(String message) {
		super(message);
	}

	public NodeExecutionException(Throwable cause) {
		super(cause);
	}
}
