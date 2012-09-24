package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutor.Component;

public class DefaultNodeInstantiator implements SpiFactory {

	private static final String POST_INIT_ACTIONS = "post-init-actions";
	
	@Override
	public Object instantiate(final ViCloudContext context, final String attrName, final AttrBag config) {
		return new Defered() {
			
			private ViNodeSpi node;
			
			@Override
			public synchronized Object getInstance() {
				if (node == null) {
					node = createNode(context, config);
				}
				return node;
			}
		};
	}

	protected ViNodeSpi createNode(ViCloudContext context, AttrBag config) {
		ExecutorProvider ep = resolveExecutorProvider(context, config);
		AdvancedExecutor.Component ae = resolveExecutor(context, config, ep);
		SimpleViNodeSpi vinode = new SimpleViNodeSpi(ae);
		List<Object> actions = new ArrayList<Object>(config.getAll(POST_INIT_ACTIONS));
		Collections.reverse(actions);
		for(Object action: actions) {
			((ViNodeAction)action).onEvent(vinode);			
		}
		return vinode;
	}

	protected AdvancedExecutor.Component resolveExecutor(ViCloudContext context, AttrBag config, ExecutorProvider ep) {
		String execId = ep.configureExecutor(config);
		AdvancedExecutor.Component ae = context.ensureNamedInstance(execId, AdvancedExecutor.Component.class);
		return ae;
	}

	protected ExecutorProvider resolveExecutorProvider(ViCloudContext context, AttrBag config) {
		String epId = config.getLast(ViSpiConsts.EXECUTOR_PROVIDER);
		ExecutorProvider ep = context.ensureNamedInstance(epId, ExecutorProvider.class);
		return ep;
	}
	
	private static class SimpleViNodeSpi extends AbstractViNodeSpi {
		
		private final AdvancedExecutor.Component executor;

		public SimpleViNodeSpi(Component executor) {
			this.executor = executor;
		}

		@Override
		public AdvancedExecutor getExecutor() {
			return executor;
		}

		@Override
		protected void destroy() {
			executor.shutdown();			
		}
	}
}
