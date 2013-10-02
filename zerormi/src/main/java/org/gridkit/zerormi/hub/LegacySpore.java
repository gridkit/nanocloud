package org.gridkit.zerormi.hub;

import org.gridkit.zerormi.DuplexStreamConnector;

/**
 * This is a legacy implementation of {@link SlaveSpore} to be used
 * with old bootstrapper.
 *   
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class LegacySpore implements SlaveSpore {
	
	private static final long serialVersionUID = 20130806L;
	
	private final String uid;
	
	public static String uidOf(SlaveSpore spore) {
		return ((LegacySpore)spore).getUID();
	}
	
	public LegacySpore(String uid) {
		this.uid = uid;
	}
	
	public String getUID() {
		return uid;
	}

	@Override
	public void start(DuplexStreamConnector masterConnector) {
		RemotingEndPoint endpoint = new RemotingEndPoint(uid, masterConnector);
		endpoint.enableHeartbeatDeatchWatch();
		endpoint.run();
	}
}
