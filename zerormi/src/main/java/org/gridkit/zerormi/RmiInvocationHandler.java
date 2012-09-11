package org.gridkit.zerormi;

import java.lang.reflect.Method;

import org.gridkit.util.concurrent.FutureEx;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
interface RmiInvocationHandler {

	public boolean isRemoteProxy(Object proxy);

	public <V> FutureEx<V> invokeRemotely(Object proxy, Method m, Object... args);

	public void exportObject(Class<?> facade, Object impl);

	public void exportObject(Class<?>[] facade, Object impl);

	public void destroy();


}
