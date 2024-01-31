package org.gridkit.nanocloud.telecontrol;

import org.gridkit.zerormi.DirectRemoteExecutor;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.hub.SlaveSpore;

public interface RemoteExecutionSession {

    public SlaveSpore getMobileSpore();

    public DirectRemoteExecutor getRemoteExecutor();

    public void setTransportConnection(DuplexStream stream);

    public void terminate(Throwable cause);
}
