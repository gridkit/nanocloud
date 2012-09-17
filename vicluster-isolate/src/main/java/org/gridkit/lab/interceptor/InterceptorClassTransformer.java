package org.gridkit.lab.interceptor;

import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InterceptorClassTransformer {
	
	private static AtomicLong CALL_SITE_ID = new AtomicLong();
	
	private static WeakHashMap<ClassLoader, InterceptorClassTransformer> CLMAP = new WeakHashMap<ClassLoader, InterceptorClassTransformer>();
	
	private ClassLoader hostClassloader;
	private List<InterceptorInfo> interceptors;
	
	public InterceptorClassTransformer(ClassLoader cl) {
		this.hostClassloader = cl;
		//CLMAP
	}
	
	private static class InterceptorInfo {
		
		String classname;
		String methodname;
		String[] signature;
		
		Interceptor interceptor;		
	}
	
}
