package org.gridkit.nanocloud.log;

import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.ZLogger;

public interface ConfigurableZLogger extends ZLogger {

	/**
	 * Sets or unsets (if level is <code>null</code>) logging level for specific path.
	 * @param path
	 * @param level
	 */
	public void setLevel(String path, LogLevel level);
	
}
