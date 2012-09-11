package org.gridkit.lab.interceptor;

import java.util.concurrent.ExecutionException;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Interception {
	
	public HookType getHookType();
	
	public Class<?> getHostClass();

	public Object getReflectionObject();
	
	/**
	 * @return <code>this</code> reference for context of interception. <code>null</code> for static method/fields and constructor access.
	 */
	public Object getThis();
	
	/**
	 * @return array of arguments, array could be modified to modify arguments passing through.
	 */
	public Object[] getCallParameters();

	/**
	 * @return result of call being hooked
	 * @throws ExecutionException
	 */
	public Object call() throws ExecutionException;
	
	/**
	 * Sets results and suppress invocation of original code
	 */
	public void setResult(Object result);

	/**
	 * Sets error and suppress invocation of original code
	 */
	public void setError(Throwable e);
	
}