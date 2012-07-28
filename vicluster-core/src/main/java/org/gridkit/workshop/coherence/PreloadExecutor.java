package org.gridkit.workshop.coherence;

import org.gridkit.gatling.firegrid.WorkExecutor;
import org.gridkit.gatling.firegrid.WorkPacket;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class PreloadExecutor implements WorkExecutor {

	private String cacheName;
	private TestDocumentGenerator generator;
	
	private transient NamedCache cache;
	private transient int packetNo;
	private transient int packetSize;
//	private transient AtomicInteger integer = new 
	
	
	@Override
	public void initialize() {
		cache = CacheFactory.getCache(cacheName);
	}

	@Override
	public void initWorkPacket(WorkPacket packet) {
		this.packetNo = packet.getPacketSeqNo();
		this.packetSize =packet.getPacketSize();		
	}

	@Override
	public int execute() {
		return 0;
	}

	@Override
	public void finishWorkPacket(WorkPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	
	
}
