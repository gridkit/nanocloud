package org.gridkit.util.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * Simple latch barrier. Latch is created in closed state and could be opened just ones.
 * 
 * This kind of barrier does not handles broken state.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class LatchBarrier implements BlockingBarrier {

	private CountDownLatch latch = new CountDownLatch(1);
	
	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		pass(true);
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException {
		latch.await();
	}

	@Override
	public void breakthrough() {
		// do nothing
	}

	@Override
	public void stepIn() {
		stepIn(false);
	}
	
	@Override
	public Future<Void> stepIn(boolean needFuture) {
		return needFuture ? new FutureLatch(latch) : null;
	}
}
