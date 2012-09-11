package org.gridkit.zerormi;

import org.gridkit.util.concurrent.FutureEx;

public interface DuplexObjectPipe {

	public void bind(ObjectReceiver receiver);
	
	public FutureEx<Void> sendObject(Object object);
	
	public void close();	
	
	public interface ObjectReceiver {
	
		public FutureEx<Void> objectReceived(Object object);
		
		public void closed();
		
	}
}
