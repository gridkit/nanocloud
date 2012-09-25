package org.gridkit.vicluster.spi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gridkit.vicluster.spi.ViCloudExtention.DeferingMode;
import org.gridkit.vicluster.spi.ViCloudExtention.DynNode;
import org.gridkit.vicluster.spi.ViCloudExtention.GroupCallMode;
import org.gridkit.vicluster.spi.ViCloudExtention.NodeCallProxy;

class NodeSpiHelper {

	public static final String POST_INIT_ACTIONS = "vinode.post-init-actions";
	
	public static void initViNodeSPI(ViNodeSpi vinode, ViCloudContext context, AttrBag config) {
		List<Object> actions = new ArrayList<Object>(config.getAll(POST_INIT_ACTIONS));
		Collections.reverse(actions);
		for(Object action: actions) {
			((ViNodeAction)action).apply(vinode);			
		}
	}

	public static MethodMode getMethodModeAnnotation(Class<?> host, Method m) {
		try {
			Method mm = host.getMethod(m.getName(), m.getParameterTypes());

			MethodMode mmm = mm.getAnnotation(MethodMode.class);
			if (mmm == null) {
				throw new IllegalArgumentException("Unknown method " + m);
			}
			return mmm;
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Unknown method " + m);
		}
	}
	
	public static <V> V createDynamicFacade(Class<V> facade, final NodeCallProxy target) {
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return target.dispatch(method, args);
			}
		};

		return Any.cast(Proxy.newProxyInstance(facade.getClassLoader(), new Class<?>[]{facade}, handler));		
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MethodMode {
		DeferingMode deferNode();
		GroupCallMode groupCallNode(); 
	}
	
}
