package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutor.Component;

public class DefaultNodeInstantiator implements SpiFactory {

	private ViCloudContext context;
	private AttrBag config;

	private ViNodeSpi node;
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		this.context = context;
		this.config = config; 
		return new Defered() {
			@Override
			public synchronized Object getInstance() {
				if (node == null) {
					node = createNode();
				}
				return node;
			}
		};
	}

	protected ViNodeSpi createNode() {
		String epId = config.getLast(ViSpiConsts.EXECUTOR_PROVIDER);
		ExecutorProvider ep = context.ensureNamedInstance(epId, ExecutorProvider.class);
		String execId = ep.configureExecutor(config);
		AdvancedExecutor.Component ae = context.ensureNamedInstance(execId, AdvancedExecutor.Component.class);
		return new SimpleViNodeSpi(ae);
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
