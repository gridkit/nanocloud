package org.gridkit.vicluster.isolate;


public interface IsolateClassTransformerSupport {
	
	public Class<?> loadIsolated(String className) throws ClassNotFoundException;

	public Class<?> defineIsolated(String className, byte[] classData) throws ClassNotFoundException;

	/**
	 * Instrumentation may need information about class hierarchy.
	 * This method is used to provide such information, allowing Isolate to utilize shared metadata cache.
	 * 
	 * @param targetClass
	 * @param questionClass
	 * @return
	 */
	public boolean isAssignable(String targetClass, String questionClass);
	
}
