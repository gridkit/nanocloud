package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;

public class DirectNodeInstantiator implements NodeProvider, CloudContextAware, SpiConfigurable {

	private ViCloudContext context;
	private AttrBag config;

	private ViNodeSPI node;
	
	@Override
	public void setContext(ViCloudContext context) {
		this.context = context;		
	}

	@Override
	public void configure(AttrBag conf) {
		this.config = conf;
	}

	@Override
	public synchronized ViNodeSPI getNode() {
		if (node == null) {
			String epId = config.getLast(ViSpiConsts.EXECUTOR_PROVIDER);
			ExecutorProvider ep = context.getNamedInstance(epId, ExecutorProvider.class);
			String execId = ep.configureExecutor(config);
			AdvancedExecutor exec = 
			
		}
		return node;
	}
	
	private static class DirectViNode implements ViNodeSPI {
		
	}
}
