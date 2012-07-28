package org.gridkit.util.vicontrol;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface VoidCallable {		

	public void call() throws Exception;
	
	@SuppressWarnings("serial")
	public static class VoidCallableWrapper implements Callable<Void>, Serializable {
		
		public final VoidCallable callable;
		
		public VoidCallableWrapper(VoidCallable callable) {
			this.callable = callable;
		}
	
		@Override
		public Void call() throws Exception {
			callable.call();
			return null;
		}
	}
}