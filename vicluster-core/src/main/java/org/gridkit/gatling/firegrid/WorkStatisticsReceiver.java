package org.gridkit.gatling.firegrid;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.gridkit.gatling.stats.PerfSampleBlock;

public interface WorkStatisticsReceiver extends Remote {

	public void sendStatistics(PerfSampleBlock block) throws RemoteException;
	
}
