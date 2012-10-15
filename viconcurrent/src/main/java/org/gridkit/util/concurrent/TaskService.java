package org.gridkit.util.concurrent;

import java.util.concurrent.TimeUnit;

public interface TaskService {

	public void schedule(Task task);

	public void schedule(Task task, long delay, TimeUnit tu);
	
	/**
	 * If {@link TaskService#schedule(Task)} method havn't thrown an
	 * exception, it is guaranteed that eigther {@link #run()} or {@link #cancled()}
	 * would be called eventually.
	 * 
	 * {@link #interrupt(Thread)} may be used to abort task execution, beware that it could be called when {@link #run()} is already finished or have yet to be started.
	 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
	 */
	public interface Task {
		
		public void run();

		public void interrupt(Thread taskThread);
		
		public void cancled();
		
	}
	
	public interface Component extends TaskService {
		
		public void shutdown();
		
	}
}
