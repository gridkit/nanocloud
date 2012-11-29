/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.util.concurrent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleTaskService implements TaskService {
	
	private ScheduledThreadPoolExecutor threadPool;
	private Set<TaskWrapper> activeTasks = new HashSet<TaskWrapper>();
	
	public SimpleTaskService(int corePoolSize) {
		threadPool = new ThreadPool(100, new WorkerThreadFactory(), new RejectionHandler());	
	}

	@Override
	public synchronized void schedule(Task task) {
		if (threadPool.isShutdown()) {
			throw new IllegalStateException("Service is shutdown");
		}
		TaskWrapper wt = new TaskWrapper(task);
		activeTasks.add(wt);
		threadPool.execute(wt);
	}

	@Override
	public synchronized void schedule(Task task, long delay, TimeUnit tu) {
		if (threadPool.isShutdown()) {
			throw new IllegalStateException("Service is shutdown");
		}
		TaskWrapper wt = new TaskWrapper(task);
		activeTasks.add(wt);
		threadPool.schedule(wt, delay, tu);
	}

	public void shutdown() {
		synchronized(this) {
			if (threadPool.isTerminated()) {
				return;
			}
			else if (threadPool.isShutdown()) {
				while(true) {
					try {
						if (threadPool.awaitTermination(10, TimeUnit.DAYS)) {
							return;
						}
					} catch (InterruptedException e) {
						// if thread is being interrupted, just let it go
						Thread.currentThread().interrupt();
						return;
					}
				}				
			}
			else {
				threadPool.shutdown();
			}
		}
		while(true) {
			TaskWrapper wrapper;
			synchronized(this) {
				if (activeTasks.isEmpty()) {
					return;
				}
				else {
					Iterator<TaskWrapper> it = activeTasks.iterator();
					wrapper = it.next();
					it.remove();
				}				
			}
			wrapper.abort();
		}
	}
	
	private class TaskWrapper implements Runnable {
		
		final Task task;

		Thread thread;		
		boolean started;
		boolean finished;
		boolean canceled;
		
		public TaskWrapper(Task task) {
			this.task = task;
		}

		@Override
		public void run() {
			if (setStarted()) {
				try {
					task.run();
				}
				catch(Throwable e) {
					shipException(e);
				}
				finally {
					setFinished();
				}
			}
		}

		private synchronized void abort() {
			if (finished) {
				return;
			}
			else {
				if (!started) {
					canceled = true;
					try {
						task.cancled();
					}
					catch(Throwable e) {
						shipException(e);
					}
				}
				else {
					try {
						task.interrupt(thread);
					}
					catch(Throwable e) {
						shipException(e);
					}
				}
			}
		}

		private void shipException(Throwable e) {
			Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
		}
		
		private synchronized boolean setStarted() {
			started = true;
			thread = Thread.currentThread();
			return !canceled;
		}
		
		private synchronized void setFinished() {
			finished = true;
			synchronized(SimpleTaskService.this) {
				activeTasks.remove(this);
			}
		}
	}
	
	private class ThreadPool extends ScheduledThreadPoolExecutor {

		public ThreadPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
			super(corePoolSize, threadFactory, handler);
		}
	}
	
	private final class RejectionHandler implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			TaskWrapper tw = (TaskWrapper) r;
			synchronized(SimpleTaskService.this) {
				activeTasks.remove(tw);
			}
			tw.abort();
		}
	}
	
	private final class WorkerThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r);
		}
	}
}
