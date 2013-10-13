/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.lab.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Interception {
	
	public HookType getHookType();
	
	public Class<?> getHostClass();

	/**
	 * Currently only methods ({@link Method}) could be intercepted.
	 * @return reflection object for this interception
	 */
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