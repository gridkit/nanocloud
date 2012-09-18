package org.gridkit.vicluster.isolate;

public interface IsolateClassTransformer {
	
	// TODO use separate interface to analyze classes instead of ClassLoader 
	public byte[] transform(ClassLoader cl, String name, byte[] originalClass);

}
