package org.gridkit.vicluster.spi;

import java.lang.reflect.Method;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViNode;

public interface ViCloudExtention<V> {
	
	enum LazyMode {
		NO_SPI_NEEDED,
		SMART_DEFERABLE,
		SPI_REQUIRED
	}
	
	public Class<V> getFacadeInterface();
	
	public LazyMode modeForMethod(Method m);
	
	public void processNodeConfig(DynNode node, AttrList config);
	
	public V wrapSingle(DynNode node);
	
	public V wrapMultiple(DynNode[] nodes);
	
	public static interface DynNode {
		
		boolean isConfigured();

		boolean isTerminated();
		
		ViNode getProxy();
		
		<V> V adapt(Class<V> facade);

		ViNodeSpi getCoreNode();

		FutureEx<ViNodeSpi> getCoreFuture();
	}
	
}
