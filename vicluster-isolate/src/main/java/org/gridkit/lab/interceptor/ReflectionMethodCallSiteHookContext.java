package org.gridkit.lab.interceptor;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class is used by synthetic byte hooks.
 * @deprecated It is not meant for direct use. 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@Deprecated
public class ReflectionMethodCallSiteHookContext implements Interception {

	private Class<?> hostClass;
	private String stubMethod;
	private String stubSignature;
	private Class<?> targetClass;
	private String targetMethod;
	private String targetMethodSignature;
	private Method method;
	private Object that;
	private Object[] parameters;
	private Object result;
	private Throwable exception;
	private boolean resultReady;
	
	@Override
	public HookType getHookType() {
		return HookType.METHOD_CALL_SITE;
	}

	@Override
	public Class<?> getHostClass() {
		return hostClass;
	}

	@Override
	public Object getReflectionObject() {
		if (method == null) {
			try {
				Class<?>[] pt = getParameterTypes(targetClass, targetMethodSignature);
				method = targetClass.getDeclaredMethod(targetMethod, pt);
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot instantiate method " + targetClass.getName() + "::" + targetMethod + targetMethodSignature, e);
			}
		}
		return method;
	}

	
	private Class<?>[] getParameterTypes(Class<?> host, String signature) throws ClassNotFoundException {
		String[] typeNames = ByteCodeHelper.parseParamTypeNames(signature);
		Class<?>[] types = new Class[typeNames.length];
		for(int i = 0; i != types.length; ++i) {
			types[i] = classforName(host, typeNames[i]);
		}
		return types;
	}

	static Class<?> classforName(Class<?> host, String type) throws ClassNotFoundException {
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
			Class<?> ct = classforName(host, type.substring(1));
			return Array.newInstance(ct, 0).getClass();
		}
		else if (type.startsWith("L") && type.endsWith(";")) {
			return classforName(host, type.substring(1, type.length() - 1));
		} else {
			ClassLoader cl = host == null ? Thread.currentThread().getContextClassLoader() : host.getClassLoader();
			return cl.loadClass(type.replace('/', '.'));
		}
	}

	@Override
	public Object getThis() {
		return that;
	}

	@Override
	public Object[] getCallParameters() {
		return parameters;
	}

	@Override
	public Object call() throws ExecutionException {
		if (resultReady) {
			throw new IllegalStateException("can be executed only once");
		}
		else {
			Method stub;
			try {
				stub = hostClass.getDeclaredMethod(stubMethod, getParameterTypes(hostClass, stubSignature));
				stub.setAccessible(true); 
			}
			catch(Exception e) {
				throw new RuntimeException("Cannot access stub method " + hostClass.getName() + "::" + stubMethod, e);
			}
			Object[] callParams = parameters;
			if (stub.getParameterTypes().length > callParams.length) {
				if (that == null) {
					throw new IllegalArgumentException("Method " + targetClass.getName() + "::" + targetMethod + targetMethodSignature + " is not static, but [this] is null");
				}
				callParams = Arrays.copyOf(callParams, callParams.length + 1);
				callParams[0] = that;
				System.arraycopy(parameters, 0, callParams, 1, parameters.length);
			}
			else {
				if (that != null) {
					throw new IllegalArgumentException("Method " + targetClass.getName() + "::" + targetMethod + targetMethodSignature + " is static, but [this] is not null");
				}
			}
			try {
				Object result;
				setResult(result = stub.invoke(null, callParams));
				return result;
			}
			catch(IllegalAccessException e) {
				throw new RuntimeException("Cannot access stub method " + hostClass.getName() + "::" + stubMethod, e);
			}
			catch(InvocationTargetException e) {
				Throwable ee = e.getCause();
				ee = ee == null ? e : ee;
				setError(ee);
				throw new ExecutionException(ee);
			}
		}
	}

	public boolean isResultReady() {
		return resultReady;	
	}
	
	public Object getResult() {
		return result;
	}
	
	public Throwable getError() {
		return exception;
	}
	
	@Override
	public void setResult(Object r) {
		result = r;
		exception = null;
		resultReady = true;
	}

	@Override
	public void setError(Throwable e) {
		result = null;
		exception = e;
		resultReady = true;
	}
	
	public void setHostClass(Class<?> hostClass) {
		this.hostClass = hostClass;
	}
	
	public void setStubMethod(String stub, String signature) {
		this.stubMethod = stub;
		this.stubSignature = signature;
	}
	
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public void setTargetMethod(String method, String signature) {
		this.targetMethod = method;
		this.targetMethodSignature = signature;
	}
	
	public void setThis(Object that) {
		this.that = that;
	}
	
	public void setParameters(Object[] params) {
		this.parameters = params;
	}
}
