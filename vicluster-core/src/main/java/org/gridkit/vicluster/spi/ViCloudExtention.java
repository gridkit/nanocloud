package org.gridkit.vicluster.spi;

import java.lang.reflect.Method;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViNode;

public interface ViCloudExtention<V> {
	
	enum DeferingMode {
		NO_SPI_NEEDED,
		SMART_DEFERABLE,
		SPI_REQUIRED
	}

	enum GroupCallMode {
		UNSUPPORTED,
		BY_IMPLEMENTATION,
		INSTANT_BROADCAST,
		STICKY_BROADCAST
	}
	
	public Class<V> getFacadeInterface();
	
	public Class<?>[] getHidenInterfaces();
	
	public DeferingMode deferingModeForMethod(Method m);

	public GroupCallMode groupModeForMethod(Method m);
	
	public void processNodeConfig(DynNode node, AttrList config);
	
	public V wrapSingle(DynNode node);
	
	public V wrapMultiple(NodeCallProxy selector, DynNode[] nodes);
	
	public static interface NodeCallProxy {
		
		Object dispatch(Method method, Object... params) throws Throwable;
	}
	
	public static interface DynNode extends NodeCallProxy {
		
		String getName();
		
		String[] getLabels();
		
		boolean isConfigured();

		boolean isTerminated();
		
		ViNode getProxy();
		
		<V> V adapt(Class<V> facade);

		/**
		 * If {@link ViNodeSpi} is available action will be applied now.
		 * Otherwise it will be applied once {@link ViNodeSpi} is initialized.
		 */
		void applyAction(ViNodeAction action);
		
		ViNodeSpi getCoreNode();

		FutureEx<ViNodeSpi> getCoreFuture();
	}	
}
