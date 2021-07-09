package org.gridkit.nanocloud.telecontrol.isolate;

import java.io.Serializable;

import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.hub.SlaveSpore;

/**
 * Wraps provided spore into Isolate to enable instrumentation. 
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class IsolatedRemoteSession implements RemoteExecutionSession {

	private RemoteExecutionSession nested;
	
	public IsolatedRemoteSession(RemoteExecutionSession nested) {
		this.nested = nested;
	}

	@Override
	public SlaveSpore getMobileSpore() {
		return new IsolatedSpore(nested.getMobileSpore());
	}

	@Override
	public AdvancedExecutor getRemoteExecutor() {
		return nested.getRemoteExecutor();
	}

	@Override
	public void setTransportConnection(DuplexStream stream) {
		nested.setTransportConnection(stream);
	}

	@Override
	public void terminate(Throwable cause) {
		nested.terminate(cause);
	}

	private static class IsolatedSpore implements SlaveSpore, Serializable {

		private static final long serialVersionUID = 20131209L;
		
		private SlaveSpore spore;
		
		public IsolatedSpore(SlaveSpore spore) {
			this.spore = spore;
		}

		@Override
		public void start(final DuplexStreamConnector masterConnector) {
			System.setProperty("gridkit.isolate.suppress.multiplexor", "true");
			Isolate isolate = new Isolate("Spore");
			isolate.exclude(DuplexStream.class, DuplexStreamConnector.class);
			final SlaveSpore spore = this.spore;
			isolate.start();
			isolate.exec(new Runnable() {
				@Override
				public void run() {
					spore.start(masterConnector);					
				}
			});
			isolate.stop();
		}
	}
}
