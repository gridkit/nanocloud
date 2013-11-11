package org.gridkit.nanocloud.log;

import java.util.Map;

import org.gridkit.zerormi.zlog.ZLogFactory;
import org.gridkit.zerormi.zlog.ZLogger;
import org.slf4j.Logger;

public class Slf4jInstantiator implements LoggerInstantiator {

	public static boolean isAvailable() {
		try {
			Logger.class.getName();
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
	
	@Override
	public ZLogger instantiate(Map<String, Object> config) {
		String root = (String) config.get("root");
		if (root == null) {
			root = "";
		}
		ZLogger zl = ZLogFactory.getSlf4JRootLogger().getLogger(root);
		if (root != null) {
			zl = zl.getLogger(root);
		}
		return zl;
	}	
}
