package org.gridkit.util.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/** 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com) 
 */
public class SimpleSpeedLimit implements BlockingBarrier {

	private static final long OPTIMISTIC_WAIT_TIMEOUT = 50;
	
	private Semaphore semaphore = new Semaphore(0, true);
	private ReentrantLock replenishLock = new ReentrantLock();
	
	private long anchorPoint;
	private int anchorDrained;
	private double eventRate; /* events per second */
	private int replenishMark;
	private int replenishLimit;
	private double burst;
	
	public SimpleSpeedLimit(double eventRate, int replenishLimit) {
		this.anchorPoint = System.nanoTime();
		this.anchorDrained = 0;
		this.eventRate = eventRate;
		this.replenishMark = replenishLimit / 2;
		this.replenishLimit = replenishLimit;
		this.burst = eventRate > 1 ? 0.3 * Math.log10(eventRate) : 0;
		this.burst += 0.3 * eventRate * OPTIMISTIC_WAIT_TIMEOUT / TimeUnit.SECONDS.toMillis(1);
	}
	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		pass(true);
	}

	@Override
	public void stepIn() {
		stepIn(false);
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException {
		accure();
	}

	@Override
	public void breakthrough() {
		return;		
	}

	@Override
	public Future<Void> stepIn(boolean needFuture) {
		if (needFuture) {
			return new FutureLatch() {

				@Override
				public synchronized Void get() throws InterruptedException, ExecutionException {
					if (!isDone()) {
						accure();
						open();
						return null;
					}
					else {
						return null;
					}
				}

				@Override
				public synchronized Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,		TimeoutException {
					if (!isDone()) {
						accure();
						open();
						return null;
					}
					else {
						return null;
					}
				}
			};
		}
		else {
			try {
				replenishLock.lock(); 
				++anchorDrained;
			}
			finally {
				replenishLock.unlock();
			}
			return null;
		}
	}

	public void accure() throws InterruptedException {
		if (semaphore.tryAcquire()) {
			if (semaphore.availablePermits() < replenishMark) {
				tryReplenish();
			}
			return;
		}
		else {
			while(true) {
				tryReplenish();
				if (semaphore.tryAcquire(OPTIMISTIC_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
					return;
				}
			}
		}
	}

	public boolean tryAccure(long timeout, TimeUnit tu) throws InterruptedException {
		if (semaphore.tryAcquire()) {
			if (semaphore.availablePermits() < replenishMark) {
				tryReplenish();
			}
			return true;
		}
		else {
			long deadline = System.nanoTime() + tu.toNanos(timeout);
			while(true) {
				tryReplenish();
				long delay = Math.min(TimeUnit.MILLISECONDS.toNanos(OPTIMISTIC_WAIT_TIMEOUT), deadline - System.nanoTime());
				if (delay <= 0) {
					return false;
				}
				if (semaphore.tryAcquire(delay, TimeUnit.NANOSECONDS)) {
					return true;
				}
			}
		}
	}

	private void tryReplenish() {
		if (replenishLock.tryLock()) {
			try {
				while(true) {
					long now = System.nanoTime();
					long split = now - anchorPoint;
					int replenishAmount = (int) (Math.ceil(eventRate * split / TimeUnit.SECONDS.toNanos(1) + burst)) - anchorDrained;
					if (replenishAmount > 0) {
						anchorDrained += replenishAmount;
						if (replenishAmount > replenishLimit) {
							replenishAmount = replenishLimit;
						}

						
//						if (anchorDrained >= 2 * replenishLimit) {
//							// reset anchor point;
//							anchorDrained = 0;
//							anchorPoint = now;
//						}
						
						semaphore.release(replenishAmount);
						return;
					}
					else {
						long sleepTime = (long)(TimeUnit.SECONDS.toNanos(1) / eventRate);
						if (sleepTime > TimeUnit.MILLISECONDS.toNanos(10)) {
							Thread.yield();
						}
						else {
							LockSupport.parkNanos(sleepTime / 5);
						}
					}
				}
			}
			finally {
				replenishLock.unlock();
			}
		}
	}	
}
