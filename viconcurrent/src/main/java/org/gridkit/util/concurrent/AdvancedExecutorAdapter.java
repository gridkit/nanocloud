package org.gridkit.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class AdvancedExecutorAdapter implements AdvancedExecutor {
	
	private final Executor executor;

	public AdvancedExecutorAdapter(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void execute(Runnable task) {
		executor.execute(task);		
	}

	@Override
	public FutureEx<Void> submit(final Runnable task) {
		final FutureBox<Void> fb = new FutureBox<Void>();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
					fb.setData(null);
				}
				catch(Throwable e) {
					fb.setError(e);
				}
			}
		});
		return fb;
	}

	@Override
	public <V> FutureEx<V> submit(final Callable<V> task) {
		final FutureBox<V> fb = new FutureBox<V>();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					fb.setData(task.call());
				}
				catch(Throwable e) {
					fb.setError(e);
				}
			}
		});
		return fb;
	}
}
