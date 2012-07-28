package org.gridkit.gatling.firegrid;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterCoordinator extends Remote {

	public void swear(Slave slave) throws RemoteException;
	
	public void ping() throws RemoteException;
	
}
