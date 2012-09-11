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
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutionException;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteStub implements InvocationHandler  {

	public static RmiInvocationHandler getRmiChannel(Object proxy) {
		return ((RemoteStub)Proxy.getInvocationHandler(proxy)).getRmiChannel();
	}
	
	private RemoteInstance identity;
	private RmiInvocationHandler channel;
	
	public RemoteStub(RemoteInstance identity, RmiInvocationHandler channel) {
		this.identity = identity;
		this.channel = channel;
	}
	
	public RemoteInstance getRemoteInstance() {
		return identity;
	}

	public RmiInvocationHandler getRmiChannel() {
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
			try {
				return channel.invokeRemotely(proxy, method, args).get();
			}
			catch(ExecutionException e) {
				throw e.getCause();
			}
		}
	}
	
	public String toString() {
		return identity.toString();
	}
}
