package org.gridkit.vicluster.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.spi.IsolateNodeInstantiator.SimpleIsolateViNodeSpi;
import org.gridkit.vicluster.telecontrol.ControlledProcess;

public class NanoNodeInstantiator implements SpiFactory {

	public static final String HOST = Host.class.getName();
	public static final String TASK_SERVICE = TaskService.class.getName();
	
	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		Host host = (Host)config.getLast(HOST);
		TaskService taskService = (TaskService)config.getLast(TASK_SERVICE);

		ControlledProcess
		
		ViNodeSpi nodeSpi = new SimpleIsolateViNodeSpi(isolate);
		NodeSpiHelper.initViNodeSPI(nodeSpi, context, config);
		
		return nodeSpi;
		
		
		
		return null;
	}
}
