package org.gridkit.workshop.coherence;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import org.gridkit.gatling.firegrid.AbstractMasterServer;

public class SimpleReadTestPlan extends AbstractMasterServer {

	public static void main(String[] args) throws RemoteException, AlreadyBoundException, InterruptedException {
		
		SimpleReadTestPlan testPlan = new SimpleReadTestPlan();
		testPlan.start();

		int keySpace = 100000;
		long offset = 10000000;
		
		GetExecutor executor = new GetExecutor("dist-v1-objects", offset, keySpace);
		
		testPlan.awaitSlaves(1);		
		WorkStreamSession session = testPlan.createWorkstream("read", executor, 2);
		System.out.println("Warming up ...");
		session.setRate(100);
		session.waitForTime(30000);
		session.setRate(2000);
		session.waitForTime(60000);
		session.setRate(4000);
		session.waitForTime(60000);
		session.setRate(6000);
		session.waitForTime(60000);
		session.setRate(8000);
		session.waitForTime(60000);
		session.setRate(10000);
		session.waitForTime(60000);
		session.setRate(15000);
		session.waitForTime(60000);
		session.stop();
		System.exit(0);
	}	
}
