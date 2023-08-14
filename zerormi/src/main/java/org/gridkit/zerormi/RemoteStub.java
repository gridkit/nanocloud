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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.objenesis.ObjenesisStd;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteStub implements InvocationHandler  {

	/**
	 * Interface to simplify interaction with Proxy instances.
	 * Methods are started with ___ to not interfere with methods of original class.
	 */
	public interface ProxyHandlerSetterGetter {
		InvocationHandler ___getHandler();
		void ___setHandler(InvocationHandler handler);
	}

	private RemoteInstance identity;
	private RmiChannel channel;
	
	public RemoteStub(RemoteInstance identity, RmiChannel channel) {
		this.identity = identity;
		this.channel = channel;
	}
	
	public RemoteInstance getRemoteInstance() {
		return identity;
	}

	public RmiChannel getRmiChannel() {
		return channel;
	}
	
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass() == Object.class) {
			try {
				return method.invoke(this, args);
			}
			catch(InvocationTargetException e) {
				throw e.getCause();
			}
		}
		else {
			return channel.remoteInvocation(this, proxy, method, args);
		}
	}
	
	public FutureEx<Object> asyncInvoke(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            FutureBox<Object> box = new FutureBox<Object>();
            try {
                box.setData(method.invoke(this, args));
            }
            catch (IllegalAccessException e) {
                box.setError(e);
            }
            catch(InvocationTargetException e) {
                box.setError(e.getCause());
            }
            return box;
        }
        else {
            return channel.asyncRemoteInvocation(this, proxy, method, args);
        }	    
	}

	@SuppressWarnings("rawtypes")
	public static Object buildProxy(RemoteInstance remoteInstance, RmiChannel channel) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(remoteInstance.className);
		Class<?> proxyClass = new ByteBuddy()
				.subclass(clazz)

				.defineField("handler", InvocationHandler.class, Visibility.PUBLIC)

				.method(not(isDeclaredBy(ProxyHandlerSetterGetter.class)))
				.intercept(InvocationHandlerAdapter.toField("handler"))

				.implement(ProxyHandlerSetterGetter.class)
				.intercept(FieldAccessor.ofField("handler"))

				.make()
				.load(RemoteStub.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
				.getLoaded();
		Object result = new ObjenesisStd().newInstance(proxyClass);
		((ProxyHandlerSetterGetter)result).___setHandler(new RemoteStub(remoteInstance, channel));
		return result;
	}

	private static InvocationHandler getInvocationHandler(Object proxy) {
		if (proxy instanceof ProxyHandlerSetterGetter) {
			return ((ProxyHandlerSetterGetter) proxy).___getHandler();
		}
		else {
			throw new IllegalArgumentException("Not a proxy instance");
		}
	}

	public static boolean isRemoteStub(Object proxy) {
        return proxy instanceof ProxyHandlerSetterGetter;
    }

	public static Class<?> getOriginalClass(Object proxy){
		if (proxy instanceof ProxyHandlerSetterGetter) {
			return proxy.getClass().getSuperclass();
		}
		else {
			throw new IllegalArgumentException("Not a proxy instance");
		}
	}
	
	@SuppressWarnings("unchecked")
    public static <T> FutureEx<T> remoteSubmit(Object proxy, Method method, Object... arguments) {
	    Object handler = getInvocationHandler(proxy);
	    if (handler instanceof RemoteStub) {
	        RemoteStub stub = (RemoteStub) handler;
	        return (FutureEx<T>) stub.asyncInvoke(proxy, method, arguments);
	    }
	    else {
	        throw new IllegalArgumentException("Not a remote proxy");
	    }
	}
	
	public String toString() {
		return identity.toString();
	}
}
