package org.gridkit.gatling.firegrid;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.formating.Formats;

public class SlaveServer implements Slave {
	
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException {
		
//		CpuUsageReporter.startReporter();
		
		String address = args.length == 0 ? "localhost" : args[0];
		System.out.println("Connecting to master [" + address + "]");
		
		Registry registry = LocateRegistry.getRegistry(address);
		final MasterCoordinator coordinator = (MasterCoordinator) registry.lookup("master");
		
		coordinator.swear(RmiHelper.exportObject(new SlaveServer()));
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true) {
					try {
						LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
						coordinator.ping();
					} catch (Exception e) {
						System.out.println("Master disconnected");
						System.exit(1);
					}
				}
			}
			
		}).start();
		
		while(true) {
			Thread.sleep(300);
		}
	}
	
	@Override
	public DaemonIdentity getIdentity() {
		return DaemonIdentity.LOCAL;
	}

	@Override
	public void createWorkStream(WorkStream workstream) {
		WorkStreamProcessor wsp = new WorkStreamProcessor(workstream);
		Thread thread = new Thread(wsp);
		thread.setName("Workstream:" + workstream.getWorkStreamId());
		thread.start();
		System.out.println(Formats.currentDatestamp() + " Workstream processor [" + workstream.getWorkStreamId() + "] has been created");
	}

	@Override
	public void terminate() {
		System.out.println(Formats.currentDatestamp() + " Node has been terminated by master");
		System.exit(1);		
	}

	@Override
	public void ping() {
	}

	private class WorkStreamProcessor implements Runnable {
		
		private WorkStream workstream;
		private ExecutorService service;
		
		public WorkStreamProcessor(WorkStream workstream) {
			this.workstream = workstream;
			this.service = Executors.newFixedThreadPool(workstream.getThreaCount());
		}

		@Override
		public void run() {
			workstream.getExecutor().initialize();
			while(true) {
				WorkPacket packet;
				
				try {
					packet = workstream.getCoordinator().fetchWorkPacket(DaemonIdentity.LOCAL);
				} catch (RemoteException e1) {
					e1.printStackTrace();
					System.exit(1);
					throw new RuntimeException();
				}
				
				if (packet == null) {
					workstream.getExecutor().shutdown();
					service.shutdown();
					System.out.println("Work stream [" + workstream.getWorkStreamId() + "] has been terminated");
					return;
				}
				else {
					try {
						processWorkPacket(packet);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}
			}
		}

		private void processWorkPacket(WorkPacket packet) throws InterruptedException {
			final BlockingBarrier limit = Barriers.speedLimit(packet.getExecutionRate());
			final AtomicInteger execCounter = new AtomicInteger(packet.getPacketSize());
			final CountDownLatch finishLatch = new CountDownLatch(workstream.getThreaCount());
			
			workstream.getExecutor().initWorkPacket(packet);
			for (int i = 0; i != workstream.getThreaCount(); ++i) {
				service.submit(new Callable<Void>() {
					
					@Override
					public Void call() throws InterruptedException, BrokenBarrierException {
						while(true) {
							if (execCounter.decrementAndGet() < 0) {
								finishLatch.countDown();
								return null;
							}
							else {
								limit.pass();
								workstream.getExecutor().execute();
							}
						}
					}
				});
			}
			finishLatch.await();
			workstream.getExecutor().finishWorkPacket(packet);
		}
	}
}
