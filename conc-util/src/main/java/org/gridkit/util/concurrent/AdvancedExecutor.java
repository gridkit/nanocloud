package org.gridkit.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;


public interface AdvancedExecutor extends Executor {

	public void execute(Runnable task);

	public FutureEx<Void> submit(Runnable task);

	public <V> FutureEx<V> submit(Callable<V> task);
	
}
