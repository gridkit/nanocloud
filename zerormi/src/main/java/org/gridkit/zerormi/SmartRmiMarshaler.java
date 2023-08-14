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
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objenesis.ObjenesisStd;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

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

	/**
	 * Return supclass of object's class implementing merker interface CreateRemoteProxy.
	 * All methods will be proxied to original class.
	 */
	public static <T> T wrapForRmiProxy(T object) {
		Class<?> proxyClass = new ByteBuddy()
				.subclass(object.getClass())
				.method(ElementMatchers.<MethodDescription>any())
				.intercept(MethodDelegation.to(object))

				.implement(CreateRemoteProxy.class)// mark as eligible to proxy-creation

				.make()
				.load(RemoteStub.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
				.getLoaded();
		//noinspection unchecked
		return (T) new ObjenesisStd().newInstance(proxyClass);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object writeReplace(Object obj) throws IOException {
		if (obj instanceof Serializable && !Proxy.isProxyClass(obj.getClass())) {
			return obj; // no marshaling
		}
		else if (obj instanceof CreateRemoteProxy){
			return new Exported(obj, obj.getClass().getSuperclass());
		}
		else if (RemoteStub.isRemoteStub(obj)){
			return new Exported(obj, RemoteStub.getOriginalClass(obj));
		}
		else if (obj instanceof Remote){
			return new Exported(obj, obj.getClass());
		}
		else {
			return SmartAnonMarshaler.marshal(obj);
		}
	}

	@Override
	public Object readResolve(Object obj) throws IOException {
		if (obj instanceof SmartAnonMarshaler.AnonEnvelop) {
			return ((SmartAnonMarshaler.AnonEnvelop)obj).unmarshal();
		}
		else {
			return obj;
		}
	}	
}
