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
package org.gridkit.zerormi.util;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteExporter {
	
	public static <T> T export(T instance, Class<T> facade, Class<?>... otherFacades) {
		Class<?>[] ifs = new Class[otherFacades.length + 1];
		ifs[0] = facade;
		System.arraycopy(otherFacades, 0, ifs, 1, otherFacades.length);
		return facade.cast(export(instance, Arrays.asList(ifs)));
	}
	
	public static Object export(Object instance, List<Class<?>> facades) {
		Handler h = new Handler(instance);
		Redirector r = new Redirector(h);
		return Proxy.newProxyInstance(facades.get(0).getClassLoader(), facades.toArray(new Class<?>[0]), r);
	}
		
	public interface RemoteInvocationHandler extends Remote {

		Object invoke(MethodInfo method, Object[] args) throws Throwable;
		
	}
	
	private static class Handler implements RemoteInvocationHandler {
		
		private final Object target;
		private final Map<MethodInfo, Method> methodCache = new ConcurrentHashMap<MethodInfo, Method>(16, 0.75f, 1);

		public Handler(Object target) {
			this.target = target;
		}

		@Override
		public Object invoke(MethodInfo method, Object[] args) throws Throwable {
			if (!methodCache.containsKey(method)) {
				Method m = method.getMethod();
				methodCache.put(method, m);
			}
			Method m = methodCache.get(method);
			return m.invoke(target, args);
		}
	}
	
	@SuppressWarnings("serial")
	private static class Redirector implements InvocationHandler, Serializable {
		
		private final RemoteInvocationHandler handler;

		public Redirector(RemoteInvocationHandler handler) {
			this.handler = handler;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				MethodInfo mi = new MethodInfo(method);
				return handler.invoke(mi, args);
			}
			catch(InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}
	
	@SuppressWarnings("serial")
	private static class MethodInfo implements Serializable {
		
		private Class<?> type;
		private String name;
		private List<Class<?>> args;

		public MethodInfo(Method m) {
			type = m.getDeclaringClass();
			name = m.getName();
			args = Arrays.asList(m.getParameterTypes());
		}

		public Method getMethod() {
			try {
				Method m = type.getMethod(name, args.toArray(new Class[0]));
				m.setAccessible(true);
				return m;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((args == null) ? 0 : args.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			MethodInfo other = (MethodInfo) obj;
			if (args == null) {
				if (other.args != null)
					return false;
			} else if (!args.equals(other.args))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
	}
}
