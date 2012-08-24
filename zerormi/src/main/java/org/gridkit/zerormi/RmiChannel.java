package org.gridkit.zerormi;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
interface RmiChannel {

	public Object remoteInvocation(RemoteStub remoteStub, Object proxy, Method method,	Object[] args) throws Throwable;

	public void close();

	public void handleMessage(RemoteMessage message);

	public <C> void exportObject(Class<C> facade, C impl);

	public Object streamResolveObject(Object obj) throws IOException;
	
	public Object streamReplaceObject(Object obj) throws IOException;

	public Class classForName(String string) throws ClassNotFoundException;

	public ClassLoader getClassLoader();

}
