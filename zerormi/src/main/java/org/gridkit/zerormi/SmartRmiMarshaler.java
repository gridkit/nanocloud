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
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SmartRmiMarshaler implements RmiMarshaler {

    @SuppressWarnings("rawtypes")
	private final Map<Class<?>, Class[]> remoteAutodetectCache = new ConcurrentHashMap<Class<?>, Class[]>();	
	
	@SuppressWarnings("rawtypes")
	private Class[] remoteInterfaceMarkers;

	public SmartRmiMarshaler() {
		remoteInterfaceMarkers = new Class[]{Remote.class};
	}

	public SmartRmiMarshaler(Class<?>... types) {
		remoteInterfaceMarkers = types;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public Object writeReplace(Object obj) throws IOException {
		if (obj instanceof Serializable && !Proxy.isProxyClass(obj.getClass())) {
			return obj; // no marshaling
		}
		else if (isEligbleForExport(obj)){
			Class[] ifs = getRemoteInterfaces(obj);
			return new Exported(obj, ifs);
		}
		else {
			return marshal(obj);
		}
	}

	@SuppressWarnings("rawtypes")
    protected boolean isEligbleForExport(Object obj) {
		for(Class marker: remoteInterfaceMarkers) {
			if (marker.isInstance(obj)) {
				return true;
			}
		}

		return false;
	}

    protected Class<?>[] getRemoteInterfaces(Object obj) throws IOException {
        Class<?> objClass = obj.getClass();
        Class<?>[] result = remoteAutodetectCache.get(objClass);
        if (result != null) {
            return result;
        } else {
            result = detectRemoteInterfaces(objClass);
            remoteAutodetectCache.put(objClass, result);
            return result;
        }
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Class<?>[] detectRemoteInterfaces(Class<?> objClass) throws IOException {
		Class<?>[] result;
		List<Class> iflist = new ArrayList<Class>();
		iflist.addAll(Arrays.asList(objClass.getInterfaces()));

		Iterator<Class> it = iflist.iterator();
		while (it.hasNext()) {
		    Class intf = it.next();

		    if (!isRemoteInterface(intf)) {
		        it.remove();
		        continue;
		    }

		    for (Class other : iflist) {
		        if (intf != other && intf.isAssignableFrom(other)) {
		            it.remove();
		        }
		    }
		}

		if (iflist.isEmpty()) {
			// no interfaces are explicitly marker as remote
			// this is a special case, assume all interfaces except Remote markers are exported
			for(Class intf: objClass.getInterfaces()) {
				if (!isRemoteInterface(intf)) {
					iflist.add(intf);
				}
			}
			
			reduceSuperTypes(iflist);
		}
		
		if (iflist.isEmpty()) {
			throw new IOException("Cannot calculate remote interface for class " + objClass.getName());
		}
		
		result = iflist.toArray(new Class[iflist.size()]);
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void reduceSuperTypes(List<Class> iflist) {
		Iterator<Class> it = iflist.iterator();
		while (it.hasNext()) {
		    Class intf = it.next();
		    for (Class other : iflist) {
		        if (intf != other && intf.isAssignableFrom(other)) {
		            it.remove();
		        }
		    }
		}	
	}

	@SuppressWarnings("rawtypes")
	private boolean isRemoteInterface(Class intf) {
		boolean remote = false;
		for (Class<?> marker : remoteInterfaceMarkers) {
		    if (marker.isAssignableFrom(intf)) {
		        remote = true;
		        break;
		    }
		}
		return remote;
	}
	
	
	@Override
	public Object readResolve(Object obj) throws IOException {
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

