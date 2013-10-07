package org.gridkit.nanocloud.telecontrol;

import java.io.IOException;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.hub.RemotingEndPoint;
import org.gridkit.zerormi.hub.SlaveSpore;

public class ZeroRmiRemoteSession implements RemoteExecutionSession {
	
	private RmiGateway gateway;
	
	public ZeroRmiRemoteSession(String nodeName) {
		// TODO logging configuration
		gateway = new RmiGateway(nodeName);		
	}
	
	@Override
	public SlaveSpore getMobileSpore() {
		Spore spore = new Spore();
		return spore;
	}

	@Override
	public AdvancedExecutor getRemoteExecutor() {
		return gateway.getRemoteExecutorService();
	}

	@Override
	public void setTransportConnection(DuplexStream stream) {
		try {
			gateway.connect(stream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void terminate() {
		gateway.shutdown();
	}
	
	public static class Spore implements SlaveSpore {
		
		private static final long serialVersionUID = 20130806L;
		
		@Override
		public void start(DuplexStreamConnector masterConnector) {
			RemotingEndPoint endpoint = new RemotingEndPoint(null, masterConnector);
			endpoint.enableHeartbeatDeatchWatch();
			endpoint.run();
		}
	}	
}
