package org.gridkit.vicluster.spi;

import java.io.IOException;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.JvmConfig;

public class NanoNodeInstantiator implements SpiFactory {

	public static final String HOST = Host.class.getName();
	public static final String TASK_SERVICE = TaskService.class.getName();
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		try {
			String name = config.getLast(AttrBag.NAME);
			Host host = (Host)config.getLast(HOST);
			TaskService taskService = (TaskService)config.getLast(TASK_SERVICE);

			AttrBag jvmConf = NanoSpiHelper.configureJvm(context, config);
			JvmConfig jconfig = (JvmConfig)jvmConf.getLast(AttrBag.INSTANCE);
			
			ControlledProcess proc = host.getProcessFactory().createProcess(name, jconfig);
			
			SimpleNanoNodeSpi nodeSpi = new SimpleNanoNodeSpi(host, proc);
			
			NodeSpiHelper.initViNodeSPI(nodeSpi, context, config);
			
			return nodeSpi;
		} catch (IOException e) {
			Any.throwUncheked(e);
		}
	}
	
	private static class SimpleNanoNodeSpi extends AbstractViNodeSpi implements RemoteNodeSpi {
		
		private final Host host;
		private final ControlledProcess proc;
		
		public SimpleNanoNodeSpi(Host host, ControlledProcess proc) {
			this.host = host;
			this.proc = proc;
		}

		@Override
		public Host getHost() {
			return host;
		}
		
		@Override
		public AdvancedExecutor getExecutor() {
			return proc.getExecutionService();
		}
		
		@Override
		protected void destroy() {
			proc.destroy();
		}
	}
}
