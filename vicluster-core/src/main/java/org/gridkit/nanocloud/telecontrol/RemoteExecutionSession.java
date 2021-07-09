package org.gridkit.nanocloud.telecontrol;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.hub.SlaveSpore;

public interface RemoteExecutionSession {
	
	public SlaveSpore getMobileSpore();
	
	public AdvancedExecutor getRemoteExecutor();

	public void setTransportConnection(DuplexStream stream);

	public void terminate(Throwable cause);
}
