package org.gridkit.util.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Future;

public class NoBarrier implements BlockingBarrier {

	public static final NoBarrier INSTANCE = new NoBarrier();

	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		// do nothing
		
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException {
		// do nothing		
	}

	@Override
	public void breakthrough() {
		// do nothing		
	}

	@Override
	public void stepIn() {
		// do nothing
	}
	
	@Override
	public Future<Void> stepIn(boolean needFuture) {
		Future<Void> result = null;
		if (needFuture) {
			FutureLatch latch = new FutureLatch();
			latch.open();
			result = latch;
		}
		return result;
	}
}
