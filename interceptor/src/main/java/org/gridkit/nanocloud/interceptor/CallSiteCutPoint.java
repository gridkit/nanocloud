/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.nanocloud.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gridkit.lab.interceptor.CutPoint;

class CallSiteCutPoint implements CutPoint {

	private static final long serialVersionUID = 20130622L;
	
	private final String[] targetClassNames;
	private final String methodName;
	private final String[] signature;
	
	public CallSiteCutPoint(String[] targetClassNames, String methodName, String[] signature) {
		this.targetClassNames = targetClassNames;
		this.methodName = methodName;
		this.signature = signature;
	}

	@Override
	public boolean evaluateCallSite(String hostClass, String hostMethod, String methodSignature, String targetClass, String targetMethod, String targetSignature) {
		if (targetClassNames != null && Arrays.binarySearch(targetClassNames, targetClass) < 0) {
			return false;
		}
		if (methodName != null && !targetMethod.equals(methodName)) {
			return false;
		}
		if (signature != null) {
			String[] sig = parseParamTypeNames(targetSignature);
			if (signature.length != sig.length) {
				return false;
			}
			for(int i = 0; i != sig.length; ++i) {
				if (signature[i] != null && !signature[i].equals(sig[i])) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static String[] parseParamTypeNames(String signature) {
		List<String> result = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		int c = signature.lastIndexOf(')');
		String types = signature.substring(1, c);
		boolean longName = false;
		for(int i = 0; i != types.length(); ++i) {
			char x  = types.charAt(i);
			if ('[' == x) {
				sb.append(x);
			}
			else if (';' == x) {
				sb.append(x);
				result.add(sb.toString());
				sb.setLength(0);
				longName = false;
			}
			else if ('L' == x) {
				sb.append(x);
				longName = true;
			}
			else if (longName){
				sb.append(x);
			}
			else {
				sb.append(x);
				result.add(sb.toString());
				sb.setLength(0);
			}
		}
		return result.toArray(new String[result.size()]);
	}	
}
