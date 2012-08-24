package org.gridkit.zerormi;

public interface ObjectPipe {

	public void bind(ObjectPipeEventHandler receiver);
	
	public void sendObject(Object object);
	
	public void close();	
	
	public interface ObjectPipeEventHandler {
	
		public void objectReceived(Object object);
		
		public void closed();
		
	}
}
