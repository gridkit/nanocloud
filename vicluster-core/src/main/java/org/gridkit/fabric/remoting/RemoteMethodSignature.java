/**
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
package org.gridkit.fabric.remoting;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

public class RemoteMethodSignature implements Serializable {
	
	private static final long serialVersionUID = 20080415L;
	
	private String className;
	private String methodName;
	private String[] methodSignature;
	
	public RemoteMethodSignature(Method method) {
		this(method.getDeclaringClass().getName(), method.getName(), toClassNames(method.getParameterTypes()));
	}

	public RemoteMethodSignature(String className, String methodName, String[] methodSignature) {
		this.className = className;
		this.methodName = methodName;
		this.methodSignature = methodSignature;
	}
	
	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public String[] getMethodSignature() {
		return methodSignature;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteMethodSignature other = (RemoteMethodSignature) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (!Arrays.equals(methodSignature, other.methodSignature))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((className == null) ? 0 : className.hashCode());
		result = prime * result
		+ ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + Arrays.hashCode(methodSignature);
		return result;
	}

	@SuppressWarnings({ "rawtypes" })
	private static String[] toClassNames(Class[] classes) {
		String[] names = new String[classes.length];
		for(int i = 0; i != classes.length; ++i) {
			names[i] = classes[i].getName();
		}
		return names;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(className).append('#').append(methodName);
		buf.append("(");
		for(String arg: methodSignature) {
			int p = arg.lastIndexOf('.');
			if (p > 0) {
				arg = arg.substring(p + 1);
			}
			buf.append(arg).append(",");
		}
		
		if (buf.charAt(buf.length() - 1) == ',') {
			buf.setCharAt(buf.length() - 1, ')');
		}
		else {
			buf.append(')');
		}
		
		return buf.toString();
	}
}
