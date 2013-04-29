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
