package org.gridkit.vicluster.telecontrol.spi;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.vicluster.spi.AbstractViNodeSpi;
import org.gridkit.vicluster.spi.Host;
import org.gridkit.vicluster.spi.RemoteNodeSpi;
import org.gridkit.vicluster.spi.ViNodeSpi;
import org.gridkit.vicluster.telecontrol.ControlledProcess;

public class JvmNodeSpi extends AbstractViNodeSpi implements ViNodeSpi, RemoteNodeSpi {

	private final Host host;
	private final ControlledProcess proc;
	private final TaskService taskService;
	private WrapperPrintStream stdOut;
	private WrapperPrintStream stdErr;
	
	public JvmNodeSpi(String name, Host host, ControlledProcess proc, TaskService taskService) {
		this.host = host;
		this.proc = proc;
		this.taskService = taskService;
		stdOut = new WrapperPrintStream("[" + name + "] ", System.out);
		stdErr = new WrapperPrintStream("[" + name + "] ", System.err);
		
		this.taskService.schedule(new StreamDumperTask(this.taskService, proc.getProcess().getInputStream(), stdOut));
		this.taskService.schedule(new StreamDumperTask(this.taskService, proc.getProcess().getErrorStream(), stdErr));
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
