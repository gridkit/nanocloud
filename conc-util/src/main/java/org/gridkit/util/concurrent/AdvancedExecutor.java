package org.gridkit.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;


public interface AdvancedExecutor extends Executor {

	public void execute(Runnable task);
	
	public FutureEx<Void> submit(Runnable task);

	public <V> FutureEx<V> submit(Callable<V> task);

	public void schedule(Runnable task, long delay, TimeUnit tu);
	
	public interface Minimal {

		public <V> FutureEx<V> submit(Callable<V> task);
		
	}

	public interface ScheduledMinimal extends Minimal {
		
		public <V> FutureEx<V> submit(Callable<V> task);
		
	}
}
