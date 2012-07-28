package org.gridkit.fabric.exec;

import java.io.IOException;

/**
 * Information need to start process.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ProcessExecutor {
	
	/**
	 * Starts process and return corresponding object.
	 */
	public Process execute(ExecCommand command) throws IOException;
}
