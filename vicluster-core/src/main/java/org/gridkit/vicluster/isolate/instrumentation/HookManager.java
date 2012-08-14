package org.gridkit.vicluster.isolate.instrumentation;


public interface HookManager {

	public String getInvocationTargetClass();
	
	public String getInvocationTargetMethod();

	/**
	 * @param className - caller class
	 * @param method - caller
	 * @param targetClass - target class
	 * @param targetMethod - target method
	 * @param targetSignature - target signature
	 * @return positive hook ID or -1 if call site not to be instrumented
	 */
	public int checkCallsite(String hostClass, String hostMethod, String methdoSignature, String targetClass, String targetMethod, String targetSignature);
	
	
}
