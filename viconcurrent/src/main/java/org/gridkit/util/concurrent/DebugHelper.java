package org.gridkit.util.concurrent;

public class DebugHelper {

	public static void enableJUnitTimeouts() {
		FutureBox.enableStoppability(true);
	}

	public static void traceTaskService() {
		SensibleTaskService.TRACE = true;
	}
	
	public static void disableDebug() {
		FutureBox.enableStoppability(false);		
		SensibleTaskService.TRACE = false;
	}
	
}
