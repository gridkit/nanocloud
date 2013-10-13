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


/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface HookManager {

	public String getInvocationTargetClass();
	
	public String getInvocationTargetMethod();

	/**
	 * @param className - caller class
	 * @param method - caller
	 * @param targetClass - target class
	 * @param targetMethod - target method
	 * @param targetSignature - target signature
	 * @return positive hook ID or -1 if call site not to be instrumented
	 */
	public int checkCallsite(String hostClass, String hostMethod, String methodSignature, String targetClass, String targetMethod, String targetSignature);
	
	
}
