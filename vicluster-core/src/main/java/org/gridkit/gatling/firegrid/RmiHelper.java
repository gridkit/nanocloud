package org.gridkit.gatling.firegrid;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class RmiHelper {

	@SuppressWarnings("unchecked")
	public static final <T extends Remote> T exportObject(T remote) throws RemoteException {
		Remote stub = UnicastRemoteObject.exportObject(remote, (new Random()).nextInt(1000) +10000);
		return (T) stub;
	}
	
}
