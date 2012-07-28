package org.gridkit.gatling.firegrid;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Slave extends Remote {
	
	public DaemonIdentity getIdentity() throws RemoteException;
	
	public void createWorkStream(WorkStream workstream) throws RemoteException;
	
	public void terminate() throws RemoteException;
	
	public void ping() throws RemoteException;

}
