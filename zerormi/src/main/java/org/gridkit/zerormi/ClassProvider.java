package org.gridkit.zerormi;

import java.lang.reflect.InvocationHandler;

public interface ClassProvider {

	public Class<?> classForName(String string) throws ClassNotFoundException;

	public Class<?> proxyClassForTypes(String[] interfaces) throws ClassNotFoundException;
	
	public Object newProxyInstance(Class<?>[] classes, InvocationHandler remoteStub);
	
}
