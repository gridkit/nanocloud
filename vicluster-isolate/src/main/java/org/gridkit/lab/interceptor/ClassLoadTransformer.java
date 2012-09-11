package org.gridkit.lab.interceptor;

public interface ClassLoadTransformer {

	public byte[] loadTransform(String className, byte[] data);
	
}
