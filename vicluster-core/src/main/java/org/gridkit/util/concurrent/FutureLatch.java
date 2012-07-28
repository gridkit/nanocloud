package org.gridkit.util.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureLatch implements Future<Void> {
	
	private CountDownLatch latch = new CountDownLatch(1);

	public FutureLatch() {
		latch = new CountDownLatch(1);
	}

	public FutureLatch(CountDownLatch latch) {
		this.latch = latch;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone()) {
			return false;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return latch.getCount() <= 0;
	}

	public void open() {
		latch.countDown();
	}
	
	@Override
	public Void get() throws InterruptedException, ExecutionException {
		latch.await();
		return null;
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (latch.await(timeout, unit)) {
			return null;
		}
		else {
			throw new TimeoutException();
		}
	}
}
