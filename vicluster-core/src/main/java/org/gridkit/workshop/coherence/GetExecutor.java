package org.gridkit.workshop.coherence;

import java.util.Random;

import org.gridkit.gatling.firegrid.WorkExecutor;
import org.gridkit.gatling.firegrid.WorkPacket;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

@SuppressWarnings("serial")
public class GetExecutor implements WorkExecutor {

	static void setProp(String propertyName, String value) {
		if (!System.getProperties().contains(propertyName)) {
			System.setProperty(propertyName, value);
		}
		System.out.println("[prop] " + propertyName + ": " + System.getProperty(propertyName));
	}	
	
	static {
		setProp("tangosol.pof.enabled", "true");
		setProp("tangosol.pof.enabled", "true");
	    setProp("tangosol.coherence.cacheconfig", "coherence-lab-cache-config.xml");
	    setProp("tangosol.coherence.distributed.localstorage", "false");
	}
	
	private static ThreadLocal<Random> threadRandom = new ThreadLocal<Random>();
	
	private String cacheName;
	private long keyOffset;
	private int keySpace;
	
	private transient NamedCache cache;
	
	@Override
	public void initialize() {		
		cache = CacheFactory.getCache(cacheName);
	}

	public GetExecutor(String cacheName, long keyOffset, int keySpace) {
		this.cacheName = cacheName;
		this.keyOffset = keyOffset;
		this.keySpace = keySpace;
	}

	@Override
	public void initWorkPacket(WorkPacket packet) {
	}

	@Override
	public int execute() {
		Random rnd = threadRandom.get();
		if (rnd == null) {
			rnd = new Random();
		}
		threadRandom.set(rnd);
		long key = keyOffset + rnd.nextInt(keySpace);
		String cacheKey = String.valueOf(key);
		Object result = cache.get(cacheKey);
		return result == null ? 1 : 0;
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
