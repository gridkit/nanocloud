package org.gridkit.nanocloud.log;

import java.util.Map;

import org.gridkit.zerormi.zlog.ZLogger;

public interface LoggerInstantiator {

	public ZLogger instantiate(Map<String, Object> config);
	
}
