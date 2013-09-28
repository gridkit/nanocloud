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
package org.gridkit.zerormi;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles serialization of anonymous inner classes using reflection.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class SmartAnonMarshaler {	
	
	public static Object marshal(Object obj) {
		if (isAnonObject(obj)) {
			return new AnonEnvelop(obj);
		}
		else {
			return obj;
		}
		
	}
	
	public static Object unmarshal(Object obj) throws IOException {
		if (obj instanceof AnonEnvelop) {
			return ((AnonEnvelop)obj).unmarshal();
		}
		else {
			return obj;
		}
	}
	
	private static boolean isAnonObject(Object obj) {
		return obj != null && (!(obj instanceof Serializable)) && obj.getClass().isAnonymousClass();
	}

	@SuppressWarnings("serial")
	public static class AnonEnvelop implements Serializable {

		private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = new HashMap<Class<?>, Object>();
		static {
			PRIMITIVE_DEFAULTS.put(boolean.class, Boolean.FALSE);
			PRIMITIVE_DEFAULTS.put(byte.class, Byte.valueOf((byte)0));
			PRIMITIVE_DEFAULTS.put(short.class, Short.valueOf((byte)0));
			PRIMITIVE_DEFAULTS.put(char.class, Character.valueOf((char)0));
			PRIMITIVE_DEFAULTS.put(int.class, Integer.valueOf((char)0));
			PRIMITIVE_DEFAULTS.put(long.class, Long.valueOf((char)0));
			PRIMITIVE_DEFAULTS.put(float.class, Float.valueOf(0f));
			PRIMITIVE_DEFAULTS.put(double.class, Double.valueOf(0f));
		}		
		
		private Class<?> type;
		private Map<String, Object> fields;
		
		public AnonEnvelop(Object instance) {
			snapshot(instance);
		}
		
		private void snapshot(Object instance) {
			try {
				type = instance.getClass();
				fields = new HashMap<String, Object>();
				Field[] ff = collectFields(type);
				for(Field f: ff) {
					if (isPersistent(f)) {
						f.setAccessible(true);
						fields.put(f.getName(), f.get(instance));
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot capture object state", e);
			}
		}

		public Object unmarshal() throws IOException {

			Constructor<?> c = type.getDeclaredConstructors()[0];
			c.setAccessible(true);
			// we have to init primitive params, cause null cannot be converted to primitive value
			Object[] params = new Object[c.getParameterTypes().length];
			for(int i = 0; i != params.length; ++i) {
				Class<?> p = c.getParameterTypes()[i];
				params[i] = PRIMITIVE_DEFAULTS.get(p);
			}

			Object oo;
			try {
				oo = c.newInstance(params);
				
				Field[] ff = collectFields(type);
				
				for(Field f : ff) {
					if (isPersistent(f)) {
						if (fields.containsKey(f.getName())) {
							f.setAccessible(true);
							Object v = fields.get(f.getName());
							f.set(oo, v);
						}
					}
				}
				return oo;
			} catch (Exception e) {
				throw new IOException(e);
			}
			
		}

		private boolean isPersistent(Field f) {
			return !f.getName().startsWith("this$") 
					&& !Modifier.isStatic(f.getModifiers()) 
					&& !Modifier.isTransient(f.getModifiers());
		}
		
		private Field[] collectFields(Class<?> c) {
			List<Field> result = new ArrayList<Field>();
			collectFields(result, c);
			return result.toArray(new Field[result.size()]);
		}
		
		private void collectFields(List<Field> result, Class<?> c) {
			Class<?> s = c.getSuperclass();
			if (s != Object.class) {
				collectFields(result, s);
			}
			for(Field f: c.getDeclaredFields()) {
				if (!Modifier.isStatic(f.getModifiers())) {
					result.add(f);
				}
			}
		}		
	}	
}
