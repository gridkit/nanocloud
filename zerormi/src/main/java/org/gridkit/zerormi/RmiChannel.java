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
import java.lang.reflect.Method;

import org.gridkit.util.concurrent.FutureEx;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
interface RmiChannel {

	public Object remoteInvocation(RemoteStub remoteStub, Object proxy, Method method, Object[] args) throws Throwable;

	public FutureEx<Object> asyncRemoteInvocation(RemoteStub remoteStub, Object proxy, Method method, Object[] args);

	public void close(Throwable cause);

	public void handleMessage(RemoteMessage message);

	public <C> void exportObject(Class<C> facade, C impl);

	public Object streamResolveObject(Object obj) throws IOException;
	
	public Object streamReplaceObject(Object obj) throws IOException;

	@SuppressWarnings("rawtypes")
	public Class classForName(String string) throws ClassNotFoundException;

	public ClassLoader getClassLoader();

}
