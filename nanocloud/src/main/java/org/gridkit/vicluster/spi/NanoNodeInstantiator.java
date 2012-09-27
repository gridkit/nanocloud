package org.gridkit.vicluster.spi;

import java.io.IOException;

import org.gridkit.util.concurrent.TaskService;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.spi.JvmNodeSpi;

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
			JvmProcessConfiguration jconfig = jvmConf.getLast(AttrBag.INSTANCE);
			
			ControlledProcess proc = host.startProcess(jconfig);
			
			JvmNodeSpi nodeSpi = new JvmNodeSpi(name, host, proc, taskService);
			
			NodeSpiHelper.initViNodeSPI(nodeSpi, context, config);
			
			return nodeSpi;
		} catch (IOException e) {
			Any.throwUncheked(e);
			throw new Error("Unreachable");
		}
	}
}
