package org.gridkit.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ExecutorHelper {

	public static class AbstractAdvancedExecutor implements AdvancedExecutor {

		
		@Override
		public void execute(Runnable task) {
			
		}

		@Override
		public FutureEx<Void> submit(Runnable task) {
			return null;
		}

		@Override
		public <V> FutureEx<V> submit(Callable<V> task) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void submit(Runnable task, Box<Void> box) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public <V> void submit(Callable<V> task, Box<V> box) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void schedule(Runnable task, long delay, TimeUnit tu) {
			// TODO Auto-generated method stub
			
		}		
	}	
}
