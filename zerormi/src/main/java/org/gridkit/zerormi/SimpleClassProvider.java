package org.gridkit.zerormi;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class SimpleClassProvider implements ClassProvider {
	
	private final ClassLoader cl;

	public SimpleClassProvider(ClassLoader cl) {
		if (cl == null) {
			throw new NullPointerException();
		}
		this.cl = cl;
	}

	@Override
	public Class<?> classForName(String type) throws ClassNotFoundException {
		if ("boolean".equals(type) || "Z".equals(type)) {
			return boolean.class;
		}
		else if ("byte".equals(type) || "B".equals(type)) {
			return byte.class;
		}
		else if ("short".equals(type) || "S".equals(type)) {
			return short.class;
		}
		else if ("char".equals(type) || "C".equals(type)) {
			return char.class;
		}
		else if ("int".equals(type) || "I".equals(type)) {
			return int.class;
		}
		else if ("long".equals(type) || "J".equals(type)) {
			return long.class;
		}
		else if ("float".equals(type) || "F".equals(type)) {
			return float.class;
		}
		else if ("double".equals(type) || "D".equals(type)) {
			return double.class;
		}
		else if ("void".equals(type) || "V".equals(type)) {
			return void.class;
		}
		else if (type.startsWith("[")) {
			Class<?> ct = classForName(type.substring(1));
			return Array.newInstance(ct, 0).getClass();
		}
		else if (type.startsWith("L") && type.endsWith(";")) {
			return classForName(type.substring(1, type.length() - 1));
		} else {
			return cl.loadClass(type.replace('/', '.'));
		}
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public Class<?> proxyClassForTypes(String[] interfaces) throws ClassNotFoundException {
		Class[] facade = new Class[interfaces.length];
		for(int i = 0; i != interfaces.length; ++i) {
			facade[i] = classForName(interfaces[i]);
		}
		return Proxy.getProxyClass(cl, facade);
	}

	@Override
	public Object newProxyInstance(Class<?>[] classes, InvocationHandler remoteStub) {
		return Proxy.newProxyInstance(cl, classes, remoteStub);
	}
}
