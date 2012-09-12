package org.gridkit.zerormi;

import org.gridkit.util.concurrent.AdvancedExecutor;

public interface RmiGateway {
	
	public boolean isRemoteProxy(Object proxy);
	
	public void exportObject(Class<?> facade, Object impl);

	public void exportObject(Class<?>[] facade, Object impl);

	public AdvancedExecutor asExecutor();
	
	public void shutdown();

}
