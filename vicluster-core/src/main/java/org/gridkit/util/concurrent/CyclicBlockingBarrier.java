package org.gridkit.util.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;

/**
 * {@link BlockingBarrier} adapter for {@link CyclicBarrier}.
 * TODO {@link Future} support is missing along with other minor semantic aspects.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CyclicBlockingBarrier implements BlockingBarrier {

	private CyclicBarrier barrier;
	
	public CyclicBlockingBarrier(int parties, Runnable action) {
		barrier = new CyclicBarrier(parties, action);
	}
	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		barrier.await();
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException {
		if (breakOnInterrupt == false) {
			throw new UnsupportedOperationException();
		}
		barrier.await();
	}

	@Override
	public void breakthrough() {
		barrier.reset();
	}

	@Override
	public void stepIn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Void> stepIn(boolean needFuture) {
		throw new UnsupportedOperationException();
	}
}
