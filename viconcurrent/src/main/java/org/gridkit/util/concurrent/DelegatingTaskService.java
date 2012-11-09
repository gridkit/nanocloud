package org.gridkit.util.concurrent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class is mostly usefully for mass canceling of tasks.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class DelegatingTaskService implements TaskService, TaskService.Component {

	private final TaskService delegate;
	private final Set<TaskWrapper> tasks = new HashSet<TaskWrapper>();
	
	private boolean terminated;
	
	public DelegatingTaskService(TaskService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void schedule(Task task) {
		TaskWrapper wrapper = new TaskWrapper(task);
		enqueue(wrapper, 0, TimeUnit.NANOSECONDS);
	}

	@Override
	public void schedule(Task task, long delay, TimeUnit tu) {
		TaskWrapper wrapper = new TaskWrapper(task);
		enqueue(wrapper, delay, tu);
	}

	private void enqueue(TaskWrapper wrapper, long delay, TimeUnit tu) {
		synchronized (this) {
			if (!terminated) {
				delegate.schedule(wrapper, delay, tu);
				return;
			}			
		}
		wrapper.abort();
	}

	@Override
	public void shutdown() {
		Set<TaskWrapper> tasks;
		synchronized(this) {
			if (terminated) {
				return;
			}
			terminated = true;
			tasks = new HashSet<TaskWrapper>(this.tasks);
		}
		for(TaskWrapper task: tasks) {
			task.abort();
		}
		synchronized (this) {
			for(TaskWrapper task: new HashSet<TaskWrapper>(this.tasks)) {
				task.abort();
			}
			tasks.clear();
		}
	}
	
	synchronized void removeTask(TaskWrapper wrapper) {
		tasks.remove(wrapper);
	}
	
	private class TaskWrapper implements Task {

		private final Task task;

		private Thread execThread;
		private boolean started = true;
		private boolean canceled = false;
		private boolean finished = false;
		
		public TaskWrapper(Task task) {
			this.task = task;
		}
		
		@Override
		public void run() {
			synchronized (this) {
				if (canceled) {
					return;
				}
				else {
					started = true;
					execThread = Thread.currentThread();
				}
				
			}
			try {
				task.run();
			}
			finally {
				synchronized (this) {
					execThread = null;
					finished = true;
				}
			}
			removeTask(this);
		}

		@Override
		public void interrupt(Thread taskThread) {
			synchronized (this) {
				if (canceled) {
					return;
				}
			}
			task.interrupt(taskThread);			
		}

		@Override
		public void cancled() {
			synchronized (this) {
				if (canceled) {
					return;
				}
				canceled = true;
			}
			removeTask(this);
			task.cancled();
		}
		
		public void abort() {
			synchronized (this) {
				if (finished || canceled) {
					return;
				}
				canceled = true;
				if (started) {
					interrupt(execThread);
					return;
				}
			}
			try {
				task.cancled();
			}
			catch(Exception e) {
				// ignore
			}
		}
	}
}
