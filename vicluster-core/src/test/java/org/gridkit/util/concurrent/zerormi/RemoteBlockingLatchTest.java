/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.util.concurrent.zerormi;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.concurrent.LatchBarrier;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class RemoteBlockingLatchTest {

	ViManager manager;

	@Before
	public void initManager() {
		manager = new ViManager(new JvmNodeProvider(new LocalJvmProcessFactory(BackgroundStreamDumper.SINGLETON)));
	}

	@After
	public void shutdownManager() {
		manager.shutdown();
		manager = null;
	}
	
	@Test(timeout = 30000)
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
