package org.gridkit.util.concurrent.zerormi;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.concurrent.LatchBarrier;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemoteBlockingLatchTest {

	ViManager manager;
	
	@Before
	public void initManager() {
		manager = new ViManager(new JvmNodeProvider(new LocalJvmProcessFactory()));
	}
	
	@After
	public void shutdownManager() {
		manager.shutdown();
		manager = null;
	}
	
	@Test(timeout = 10000)
	public void latch_barrier_ping_pong_test() throws InterruptedException, ExecutionException {
		
		LatchBarrier localBarrier = new LatchBarrier();
		final BlockingBarrier exportedBarrier = ExportableBarrier.export(localBarrier);
		
		BlockingBarrier remoteBarrier = manager.node("remote").exec(new Callable<BlockingBarrier>() {
			@Override
			public BlockingBarrier call() throws Exception {
				final LatchBarrier barrier = new LatchBarrier();
				Thread thread = new Thread() {
					@Override
					public void run() {
						System.out.println("Enter remote barrier");
						try {
							exportedBarrier.pass();
						} catch (Exception e) {
							System.err.println(e.toString());
						}
						System.out.println("Passed, opening latch");
						barrier.open();
						System.out.println("Latch is open");
					}
				};
				thread.start();
				return ExportableBarrier.export(barrier);
			}
		});
		
		System.out.println("Entering barrier");
		Future<?> passing = remoteBarrier.stepIn(true);
		Assert.assertFalse("Future should not be completed", passing.isDone());
		localBarrier.open();
		passing.get();
		System.out.println("Barrier have been passed");
	}	
}
