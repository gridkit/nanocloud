package org.gridkit.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple future which becomes "complete" by timer.
 * Cannot be canceled.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class TimedFuture implements Future<Void> {

	private static long ANCHOR = System.nanoTime();
	
	private final long nanodeadline;
	
	public TimedFuture(long nanodeadline) {
		this.nanodeadline = nanodeadline - ANCHOR;		
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return (System.nanoTime() - ANCHOR) > nanodeadline;
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException {
		await(Long.MAX_VALUE >> 1, TimeUnit.NANOSECONDS);
		return null;
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		await(timeout,unit);
		if (!isDone()) {
			throw new TimeoutException();
		}
		return null;
	}

	private void await(long timeout, TimeUnit unit) throws InterruptedException {
		long waitdeadline = System.nanoTime() - ANCHOR + unit.toNanos(timeout);
		if (waitdeadline > nanodeadline) {
			waitdeadline = nanodeadline;
		}
		while(true) {
			long sleep = waitdeadline - (System.nanoTime() - ANCHOR);
			if (sleep <=0) {
				break;
			}
			else {
				Thread.sleep(TimeUnit.NANOSECONDS.toMillis(sleep));
			}
		}
	}
}
