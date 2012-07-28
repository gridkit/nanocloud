package org.gridkit.util.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Future;

public interface BlockingBarrier {
	
	
	/**
	 * Block until it could pass barrier.
	 * {@link InterruptedException} will during wait will make barrier broken. 
	 */
	public void pass() throws InterruptedException, BrokenBarrierException;

	/**
	 * Block until it could pass barrier.
	 * @param breakOnInterrupt - whenever {@link InterruptedException} should switch barrier in broken state. 
	 */
	public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException;
	
	/**
	 * Pass barrier without blocking. If barrier is not open, it will be broken.
	 */
	public void breakthrough();

	/**
	 * Enter barrier by do not wait for passing though.
	 */
	public void stepIn();

	/**
	 * Enter barrier but receive passing {@link Future} instead of blocking.
	 */
	public Future<Void> stepIn(boolean needFuture);

}
