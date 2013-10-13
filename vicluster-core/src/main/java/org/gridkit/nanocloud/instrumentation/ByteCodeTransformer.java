package org.gridkit.nanocloud.instrumentation;

/**
 * This is an SPI for module handling byte code instrumentation.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ByteCodeTransformer {

	public byte[] rewriteClassData(String className, byte[] byteCode, HierarchyGraph graph);
	
	public static interface HierarchyGraph {
		
		public boolean isDescendant(String descendantClassName, String ascendantClassName);
		
	}
}
