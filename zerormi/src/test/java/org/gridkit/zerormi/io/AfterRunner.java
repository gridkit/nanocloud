package org.gridkit.zerormi.io;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.gridkit.util.concurrent.FutureBox;

public class AfterRunner<V> extends FutureTask<V> {

	public static AfterRunner<Void> create(Runnable task) {
		return new AfterRunner<Void>(FutureBox.dataFuture(null), task);
	}

	public static <V> AfterRunner<V> create(Callable<V> task) {
		return new AfterRunner<V>(FutureBox.dataFuture(null), task);
	}

	public static AfterRunner<Void> create(Future<Void> barrier, Runnable task) {
		return new AfterRunner<Void>(barrier, task);
	}

	public static <V> AfterRunner<V> create(Future<Void> barrier, Callable<V> task) {
		return new AfterRunner<V>(barrier, task);
	}

	private final Future<?> barrier;
	
	public AfterRunner(Future<?> barrier, Callable<V> callable) {
		super(callable);
		this.barrier = barrier;
		Thread thread = new Thread(this);
		thread.setName(callable.toString());
		thread.setDaemon(true);
		thread.start();
	}

	public AfterRunner(Future<?> barrier, Runnable runnable) {
		super(runnable, null);
		this.barrier = barrier;
		Thread thread = new Thread(this);
		thread.setName(runnable.toString());
		thread.setDaemon(true);
		thread.start();
	}
	
	@Override
	public void run() {
		try {
			barrier.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			// ignore
		}
		super.run();
	}
	
	public V join() {
		try {
			return get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			AfterRunner.<RuntimeException>anyThrow(e.getCause());
			throw new Error("Unreachable");
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void anyThrow(Throwable exception) throws E {
		throw (E)exception;
	}
}
