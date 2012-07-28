package org.gridkit.gatling.firegrid;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkStreamCoordinator extends Remote {

	public WorkPacket fetchWorkPacket(DaemonIdentity identity) throws RemoteException;
	
}
