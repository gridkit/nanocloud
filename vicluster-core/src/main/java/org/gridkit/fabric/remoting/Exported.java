package org.gridkit.fabric.remoting;

/**
 * Special class used to mark object to be exported and replaced by stub.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class Exported {

	private Class<?>[] interfaces;
	private Object object;
	
	public Exported(Object object, Class<?>... interfaces) {
		this.object = object;
		this.interfaces = interfaces;
	}

	public Class<?>[] getInterfaces() {
		return interfaces;
	}

	public Object getObject() {
		return object;
	}
}
