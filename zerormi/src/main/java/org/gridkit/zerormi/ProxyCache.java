package org.gridkit.zerormi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyCache {

	private static volatile Map<List<Class<?>>, Constructor<?>> classCache = new HashMap<List<Class<?>>, Constructor<?>>(); 
	private static volatile Map<>, Method> methodCache = new HashMap<List<Class<?>>, Method>(); 
	
	
	public static class AbstractProxyFacade {
		
		protected Method 
		
	}
}
