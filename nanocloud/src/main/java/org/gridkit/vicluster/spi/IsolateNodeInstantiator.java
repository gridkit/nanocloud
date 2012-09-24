package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutor.Component;
import org.gridkit.vicluster.isolate.Isolate;

public class IsolateNodeInstantiator extends DefaultNodeInstantiator {

	@Override
	protected Component resolveExecutor(ViCloudContext context, AttrBag config,	 ExecutorProvider ep) {
		return super.resolveExecutor(context, config, ep);
	}

	@Override
	protected ExecutorProvider resolveExecutorProvider(ViCloudContext context, AttrBag config) {
		return null;
	}
	
	private static class IsolateExecutor implements AdvancedExecutor.Component {
		
		public Isolate isolate;
		
		public IsolateExecutor(String name) {
			this.isolate = new Isolate();
		}
		
	}
}
