package org.gridkit.lab.interceptor;

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
	private Class<?> targetClass;
	private String targetMethod;
	private String targetMethodSignature;
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
		// TODO implement
		return null;
	}

	@Override
	public Object getReflectionObject() {
		// TODO implement
		return null;
	}

	@Override
	public Object[] getArguments() {
		return parameters;
	}

	@Override
	public Object call() throws ExecutionException {
		throw new UnsupportedOperationException();
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
	
	public void setStubMethod(String stub) {
		this.stubMethod = stub;
	}
	
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public void setTargetMethod(String method) {
		this.targetMethod = method;
	}
	
	public void setTargetMethodSignature(String signature) {
		this.targetMethodSignature = signature;
	}
	
	public void setParameters(Object[] params) {
		this.parameters = params;
	}
}
