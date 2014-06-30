package org.gridkit.nanocloud;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NodeConfigurationException extends NanocloudException {

	private static final long serialVersionUID = 20140118L;

	public NodeConfigurationException() {
		super();
	}

	public NodeConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public NodeConfigurationException(String message) {
		super(message);
	}

	public NodeConfigurationException(Throwable cause) {
		super(cause);
	}
}
