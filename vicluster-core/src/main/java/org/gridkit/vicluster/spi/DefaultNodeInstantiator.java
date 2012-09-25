package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutor.Component;

public class DefaultNodeInstantiator implements SpiFactory {

	@Override
	public Object instantiate(final ViCloudContext context, final String attrName, final AttrBag config) {
		return createNode(context, config);
	}

	protected ViNodeSpi createNode(ViCloudContext context, AttrBag config) {
		ExecutorProvider ep = resolveExecutorProvider(context, config);
		AdvancedExecutor.Component ae = resolveExecutor(context, config, ep);
		SimpleViNodeSpi vinode = new SimpleViNodeSpi(ae);
		NodeSpiHelper.initViNodeSPI(vinode, context, config);
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
