package org.gridkit.vicluster.isolate;

public interface IsolateClassTransformer {
	
	public void init(Isolate host, IsolateClassTransformerSupport support);
	
	// TODO use separate interface to analyze classes instead of ClassLoader 
	public byte[] transform(ClassLoader cl, String name, byte[] originalClass);

}
