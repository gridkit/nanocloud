package org.gridkit.gatling.firegrid;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Ignore;

@Ignore
public class TestSlaveServer {

	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException {
		SlaveServer.main(new String[]{});
	}
	
}
