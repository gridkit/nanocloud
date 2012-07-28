package org.gridkit.gatling.firegrid;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.Executors;

import org.gridkit.gatling.firegrid.AbstractMasterServer.WorkStreamSession;
import org.gridkit.gatling.firegrid.test.DummyWorkExecutor;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MasterSlaveTest {

	@Test()
	public void connectionTest() throws RemoteException, AlreadyBoundException, NotBoundException, InterruptedException {
		TestMaster master = new TestMaster();
		master.start();
		
		Executors.newFixedThreadPool(1).submit(new Runnable() {
			@Override
			public void run() {
				try {
					SlaveServer.main(new String[]{"localhost"});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
				
		master.awaitSlaves(1);
		WorkStreamSession session = master.createWorkstream("DummyWorkstream", new DummyWorkExecutor(), 16);
		session.setRate(100);
		Thread.sleep(30000);
		session.setRate(200);
		Thread.sleep(30000);
		session.setRate(300);
		Thread.sleep(30000);
		session.stop();
		
		System.out.println("Session terminated");

		System.out.println("Adding seconds slave");
		
		master.awaitSlaves(2);
		
		session = master.createWorkstream("DummyWorkstream", new DummyWorkExecutor(), 16);
		session.setRate(100);
		Thread.sleep(30000);
		session.setRate(200);
		Thread.sleep(30000);
		session.stop();
		
		System.out.println("Session terminated");
	}
	
	public static class TestMaster extends AbstractMasterServer {

		
	};
}
