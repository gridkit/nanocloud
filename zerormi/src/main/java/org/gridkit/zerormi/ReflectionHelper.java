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
package org.gridkit.zerormi;

import java.util.HashMap;
import java.util.Map;

class ReflectionHelper {
	
	private static Map<String, Class<?>> PRIMITIVES = new HashMap<String, Class<?>>();
	
	static {
		PRIMITIVES.put("void", void.class);
		PRIMITIVES.put("byte", byte.class);
		PRIMITIVES.put("char", char.class);
		PRIMITIVES.put("double", double.class);
		PRIMITIVES.put("float", float.class);
		PRIMITIVES.put("int", int.class);
		PRIMITIVES.put("long", long.class);
		PRIMITIVES.put("short", short.class);
		PRIMITIVES.put("boolean", boolean.class);
	}
	
	public static Class<?> primitiveToClass(String name) {
		return PRIMITIVES.get(name);
	}

}
