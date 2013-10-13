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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.lab.interceptor.ConstPoolParser.ConstEntry;
import org.gridkit.lab.interceptor.ConstPoolParser.ConstFieldOrMethod;

public class ViInstrumentation {
	
	public static CutPoint methodCutPoint(String className, String method, String... arguments) {
		String typeName = className.replace('.', '/');
		String elementType = buildMethodSpec(arguments);
		return new CutPoint(HookType.METHOD_CALL_SITE, typeName, method, elementType);
	}

	public static CutPoint methodCutPoint(Class<?> type, String method, Class<?>... arguments) {
		String className = type.getName();
		String[] tarsg = new String[arguments.length];
		for(int i = 0; i != arguments.length; ++i) {
			tarsg[i] = arguments[i].getName();
		}
		return methodCutPoint(className, method, tarsg);
	}

	private static String buildMethodSpec(String[] arguments) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for(String arg: arguments) {
			sb.append(toSigName(arg));
		}
		sb.append(")");
		return sb.toString();
	}

	private static Map<String, String> SIG_TYPE_MAP = new HashMap<String, String>();
	static {
		SIG_TYPE_MAP.put("void", "V");
		SIG_TYPE_MAP.put("byte", "B");
		SIG_TYPE_MAP.put("char", "C");
		SIG_TYPE_MAP.put("double", "D");
		SIG_TYPE_MAP.put("float", "F");
		SIG_TYPE_MAP.put("int", "I");
		SIG_TYPE_MAP.put("long", "J");
		SIG_TYPE_MAP.put("short", "S");
		SIG_TYPE_MAP.put("boolean", "Z");
	}
	
	private static Object toSigName(String type) {
		if (SIG_TYPE_MAP.containsKey(type)) {
			type = SIG_TYPE_MAP.get(type);
		}
		if (type.length() > 1 && type.charAt(0) != '[') {
			type = "L" + type + ";";
		}
		return type.replace('.', '/');
	}

	/**
	 * Parses Java class file, inspects constant pool and identifies cut points referenced in class.
	 * @param classData - binary class data
	 * @param cutPoints - set of cut points
	 * @return subset of cut points matching class 
	 */
	static Collection<CutPoint> match(byte[] classData, Collection<CutPoint> cutPoints) {
		List<CutPoint> result = new ArrayList<ViInstrumentation.CutPoint>();
		Map<String, List<CutPoint>> fqns = new HashMap<String, List<CutPoint>>();
		for (CutPoint cp: cutPoints) {
			String fqn = cp.className + "::" + cp.elementName;
			if (!fqns.containsKey(fqn)) {
				fqns.put(fqn, new ArrayList<CutPoint>(4));
			}
			fqns.get(fqn).add(cp);
		}
		ConstPoolParser parser = new ConstPoolParser(classData);
		for(ConstEntry ce: parser.all()) {
			if (ce instanceof ConstFieldOrMethod) {
				ConstFieldOrMethod ref = (ConstFieldOrMethod) ce;
				String fqn = ref.getClassName() + "::" + ref.getName();
				if (fqns.containsKey(fqn)) {
					String type = ref.getType();
					String stype = type;
					// stripping off return type
					int c = type.lastIndexOf(')');
					if (c > 0) {
						stype = type.substring(0, c + 1);
					}
					for (CutPoint cp : fqns.get(fqn)) {
						if (stype.equals(cp.elementType) || type.equals(cp.elementType)) {
							result.add(cp);
						}
					}
				}
			}
		}
		return result;
	}
	
	@SuppressWarnings("serial")
	public static class CutPoint implements Serializable {
		
		private HookType type;
		private String className;
		private String elementName;
		private String elementType;
		
		CutPoint(HookType type, String className, String elementName, String elementType) {
			this.type = type;
			this.className = className;
			this.elementName = elementName;
			this.elementType = elementType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((className == null) ? 0 : className.hashCode());
			result = prime * result
					+ ((elementName == null) ? 0 : elementName.hashCode());
			result = prime * result
					+ ((elementType == null) ? 0 : elementType.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CutPoint other = (CutPoint) obj;
			if (className == null) {
				if (other.className != null)
					return false;
			} else if (!className.equals(other.className))
				return false;
			if (elementName == null) {
				if (other.elementName != null)
					return false;
			} else if (!elementName.equals(other.elementName))
				return false;
			if (elementType == null) {
				if (other.elementType != null)
					return false;
			} else if (!elementType.equals(other.elementType))
				return false;
			if (type != other.type)
				return false;
			return true;
		}


		@Override
		public String toString() {
			switch(type) {
			case METHOD_CALL_SITE:
				return "CALLSITE[" + className + "::" + elementName + elementType + "]";
			default:
				return type + "[" + className + "::" + elementName + elementType + "]";
			}
		}
	}	
}
