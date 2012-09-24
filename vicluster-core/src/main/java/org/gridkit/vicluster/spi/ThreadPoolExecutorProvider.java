package org.gridkit.vicluster.spi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.AdvancedExecutorAdapter;

public class ThreadPoolExecutorProvider implements ExecutorProvider, CloudContextAware, SpiConfigurable {

	private static final String EXECUTOR_TYPE = AdvancedExecutor.Component.class.getName();
	private ViCloudContext context;
	private AttrBag config;
	
	@Override
	public void setContext(ViCloudContext context) {
		this.context = context;
	}

	@Override
	public void configure(AttrBag config) {
		this.config = config;		
	}

	@Override
	public String configureExecutor(AttrBag nodeConfig) {
		String nodeName = nodeConfig.getLast(AttrBag.NAME);
		if (nodeName == null) {
			throw new IllegalArgumentException("Name is empty");
		}
		AttrList proto = new AttrList();
		proto.addAll(nodeConfig, AttrBag.NAME, AttrBag.LABEL);
		proto.add(AttrBag.TYPE, EXECUTOR_TYPE);
		proto.add(AttrBag.INSTANCE, new ThreadPoolInstantiator());
	
		Selector s = Selectors.name(nodeName, EXECUTOR_TYPE);
		
		context.ensureResource(s, proto);
		
		return nodeName;
	}
	
	private static class ThreadPoolInstantiator implements SpiFactory {
		@Override
		public Object instantiate(ViCloudContext context, String attrName,	AttrBag config) {
			return new ExecutorComponent(Executors.newCachedThreadPool());
		}		
	}
	
	private static class ExecutorComponent extends AdvancedExecutorAdapter implements AdvancedExecutor.Component {

		private ExecutorService executor;
		
		public ExecutorComponent(ExecutorService executor) {
			super(executor);
		}

		@Override
		public void shutdown() {
			executor.shutdown();
		}
	}
}
