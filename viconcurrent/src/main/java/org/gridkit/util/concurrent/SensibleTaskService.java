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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Practice have shown that standard java thread pools are total
 * crap then you are dealing with complex multi-agent system.
 * </p>
 * <p>
 * They doesn't solve problem of thread sharing between activities 
 * either producing crapload of threads or leave you deadlock prone
 * due to lack of threads.
 * </p>
 * <p>
 * Ironically thread per action approach end up being much more practical.
 * </p>
 * <p>
 * Here is may attempt for create dead lock resistant by economic
 * thread scheduler.
 * </p>
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SensibleTaskService implements TaskService.Component {

	static boolean TRACE = false;

	private static class Shared {
		static TaskService INSTANCE = new SensibleTaskService(new DefaultConfig("SharedTaskService") {

			@Override
			public ThreadFactory getThreadFactory() {
				return new ThreadFactory() {
					
					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new Thread(r);
						thread.setDaemon(true);
						return thread;
					}
				};
			}
		});
	}
	
	public static TaskService getShareInstance() {
		return Shared.INSTANCE;
	}
	
	private static long TIME_ANCHOR = System.nanoTime();
	private static long EON = TimeUnit.DAYS.toNanos(1);
	
	private static long now() {
		return System.nanoTime() - TIME_ANCHOR;
	}
	
	public interface Config {
		
		public String getName();
		
		public float getSoftCoreCap();
		public float getHardCoreCap();
		public long getDelayFactor();
		public int getStandbyCap();
		
		public long getStatsUpdatePeriod();
		
		public ThreadFactory getThreadFactory();
		
	}
	
	public static class DefaultConfig implements Config {

		private String name;
		
		public DefaultConfig(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
		
		@Override
		public float getSoftCoreCap() {
			return 1;
		}

		@Override
		public float getHardCoreCap() {
			return (float)Runtime.getRuntime().availableProcessors();
		}

		@Override
		public long getDelayFactor() {
			return TimeUnit.MILLISECONDS.toNanos(500);
		}

		@Override
		public int getStandbyCap() {
			// thread operations are expensive
			// while idle threads are cheap
			return 2 * Runtime.getRuntime().availableProcessors();
		}

		@Override
		public long getStatsUpdatePeriod() {
			return TimeUnit.MILLISECONDS.toNanos(500);
		}

		@Override
		public ThreadFactory getThreadFactory() {
			return Executors.defaultThreadFactory();
		}
	}
		
	// configuartion
	private final String name;
	private final ThreadFactory factory;

	private float softCoreCap;
	private float hardCoreCap;
	private long delayFactor;
	private int standByCap; 	
	
	private long statsUpdatePeriod;
	
	// dynamic state
	private volatile float utilizationFactor;
	
	private AtomicInteger idleThreads = new AtomicInteger();
	
	private int threadIdCounter = 1;
	private final PriorityQueue<TaskUnit> queue = new PriorityQueue<SensibleTaskService.TaskUnit>(); 
	private final Map<Long, Worker> threads = new ConcurrentHashMap<Long, Worker>(8, 4);

	private final ReentrantLock queueLock = new ReentrantLock();
	private final ReentrantLock controlLock = new ReentrantLock(false);
		
	/** This signal is used to wake up balancer thread */
	private final Semaphore controlSignal = new Semaphore(0);
	private final Semaphore scheduleSignal = new Semaphore(1);
	
	private volatile long nextControlerTick = now();
	private Controler controler;
	
	private volatile boolean terminated;
	
	public SensibleTaskService(String name) {
		this(new DefaultConfig(name));
	}
	
	public SensibleTaskService(Config config) {
		name = config.getName();
		
		softCoreCap = config.getSoftCoreCap();
		hardCoreCap = config.getHardCoreCap();
		delayFactor = config.getDelayFactor();
		standByCap = config.getStandbyCap();
		statsUpdatePeriod = config.getStatsUpdatePeriod();
		
		factory = config.getThreadFactory();
		
		startControler();
	}
	
	private void startControler() {
		controler = new Controler();
		Thread thread = factory.newThread(controler);
		controler.thread = thread;
		thread.start();
	}
	
	@Override
	public void schedule(Task task) {
		schedule(task, 0, TimeUnit.NANOSECONDS);
	}

	@Override
	public void schedule(Task task, long delay, TimeUnit tu) {
		long scheduledTime = now() + tu.toNanos(delay);
		
		TaskUnit unit = new TaskUnit(scheduledTime, task);
		enqueue(unit);
		checkThreadDemand(false);
	}

	public void shutdown() {
		if (terminated) {
			// TODO wait for termination
			return;
		}
		terminated = true;
		scheduleSignal.release(Integer.MAX_VALUE >> 1);
		queueLock.lock();
		try {
			while(!queue.isEmpty()) {
				TaskUnit tu = queue.poll();
				if (tu != null) {
					tu.abort();
				}
			}			
		}
		finally {
			queueLock.unlock();
		}
		controlSignal.release();
		try {
			controler.thread.join();
		} catch (InterruptedException e) {
			// TODO logging
			// ignore
		}
		controlLock.lock();
		try {
			for(Worker worker: threads.values()) {
				TaskUnit tu = worker.currentTask;
				if (tu != null) {
					tu.abort();
				}
				// TODO there is a small chance that
				// some Task will miss abort call
			}
		}
		finally {
			controlLock.unlock();
		}
	}
	
	private void checkThreadDemand(boolean isControler) {
		
		TaskUnit tu = peekTask();
		if (tu == null) {
			return;
		}
		long nextScheduled = tu.scheduled;
		
		if (nextScheduled > now()) {
			if (nextControlerTick > nextScheduled) {
				kickControlThread();
				return;
			}
		}
		else {
			// we have task ready for execution
			if (idleThreads.get() > 0) {
				// somebody is idle, let him care
				scheduleSignal.release();
				return;
			}
			else {
				if (!isControler) {
					if (!controlLock.tryLock()) {
						kickControlThread();
						return;
					}
				}
				else {
					controlLock.lock();
				}
				try {
					if (isUnderutilized() || (isBelowHardLimit() && isOverdue(tu.scheduled))) {
						spawnWorker();
					}	
				}
				finally {
					controlLock.unlock();
				}
			}
		}
	}
	
	private boolean isUnderutilized() {
		float uf = utilizationFactor;
		if (softCoreCap > uf) {
			if (TRACE) {
				System.out.println("Effective usage: " + uf + " (cap " + softCoreCap + ")");
			}
			return true;
		}
		else {
			return false;
		}
	}

	private boolean isBelowHardLimit() {
		float uf = utilizationFactor;
		if (hardCoreCap > uf) {
			if (TRACE) {
				System.out.println("Effective usage: " + uf + " (hard cap " + softCoreCap + ")");
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean isOverdue(long scheduleTime) {
		return scheduleTime + delayFactor < now();
	}
	
	private void spawnWorker() {
		controlLock.lock();
		try {
			Worker worker = new Worker();
			Thread thread = factory.newThread(worker);
			worker.thread = thread;
			threads.put(thread.getId(), worker);
			worker.workerNo = threadIdCounter;
			utilizationFactor += 1;
			idleThreads.incrementAndGet();
			if (TRACE) {
				System.out.println("Worker " + threadIdCounter + " spawned, usage: " + utilizationFactor);
			}
			++threadIdCounter;
			thread.start();			
		}
		finally{
			controlLock.unlock();
		}
	}
	
	private void enqueue(TaskUnit unit) {
		if (terminated){
			unit.abort();
		}
		enqueueTask(unit);
		if (terminated){
			unit.abort();
			queueLock.lock();
			try {			
				queue.remove(unit);
			}
			finally {
				queueLock.unlock();
			}
		}		
	}
	
	private void enqueueTask(TaskUnit unit) {
		queueLock.lock();
		try {
			queue.add(unit);
		}
		finally {
			queueLock.unlock();
		}
	}
	
	private TaskUnit pollTask() {
		queueLock.lock();
		try {
			TaskUnit tu = queue.peek();
			if (tu == null || tu.scheduled > now()) {
				return null;
			}
			else {
				return queue.poll();
			}
		}
		finally {
			queueLock.unlock();
		}
	}

	private TaskUnit peekTask() {
		queueLock.lock();
		try {
			return queue.peek();
		}
		finally {
			queueLock.unlock();
		}
	}
	
	/**
	 * Signals control thread to wake up as soon as possible
	 */
	private void kickControlThread() {
		controlSignal.release();
	}
	
	private static class TaskUnit implements Runnable, Comparable<TaskUnit> {
		
		final Task task;

		long scheduled;
		Thread thread;		
		boolean started;
		boolean finished;
		boolean canceled;
		
		public TaskUnit(long scheduled, Task task) {
			this.task = task;
			this.scheduled = scheduled;
		}

		public int compareTo(TaskUnit o) {
			long c = scheduled - o.scheduled;
			return c > 0 ? 1 : c < 0 ? -1 : 0;
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

		synchronized void abort() {
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
					if (!canceled) {
						try {
							canceled = true;
							task.interrupt(thread);
						}
						catch(Throwable e) {
							shipException(e);
						}
					}
				}
			}
		}

		private void shipException(Throwable e) {
			// TODO
			Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
		}
		
		private synchronized boolean setStarted() {
			started = true;
			thread = Thread.currentThread();
			return !canceled;
		}
		
		private synchronized void setFinished() {
			finished = true;
		}
	}

	private class Controler implements Runnable {

		private Thread thread;
		@SuppressWarnings("unused")
		private String originalThreadName;
		
		private long nextStatsUpdate;

		@Override
		public void run() {
			originalThreadName = thread.getName();
			thread.setName(name + ":coordinator");

			nextStatsUpdate = now();
			controlSignal.release();
			while(!terminated) {
				
				boolean shouldUpdateStatistics = nextStatsUpdate <= now();
				
				if (shouldUpdateStatistics) {
					updateUtilization();
				}

				TaskUnit tu = peekTask();
				long nextScheduled = tu == null ? now() + EON : tu.scheduled;				

				boolean hasPendingTasks = nextScheduled <= now();
				nextControlerTick = Math.min(nextScheduled, nextStatsUpdate);
								
				if (hasPendingTasks) {
					if (idleThreads.get() > 0) {
						scheduleSignal.release();
					}
					else {
						checkThreadDemand(true);
					}
				}
				
				long sleepTime = nextControlerTick - now();

				if (sleepTime > 0) {
					sleep(sleepTime);
				}				
			}
		}

		private void updateUtilization() {
			controlLock.lock();
			try {
				float utilization = 0;
				for(Worker worker: threads.values()) {
					float wu = worker.collectUsage();
					utilization += wu;
				}
				if (TRACE) {
					System.out.println("New usage: " + utilization + " (idle: " + idleThreads.get() + ")");
				}
				utilizationFactor = utilization;
				nextStatsUpdate = now() + statsUpdatePeriod;
			}
			finally {
				controlLock.unlock();
			}
		}
		
		private void sleep(long sleepTime) {
			try {
				controlSignal.tryAcquire(sleepTime, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				// ignore
			}
			controlSignal.drainPermits();
		}
	}
	
	private class Worker implements Runnable {
		
		private int workerNo; 
		private Thread thread;
		@SuppressWarnings("unused")
		private String originalThreadName; 
		
		private int taskNo = 0; 
		@SuppressWarnings("unused")
		private long started;
		private volatile TaskUnit currentTask;
		
		// stats
		private int lastTask;
		private long lastCheck;
		private int encounters;
		private boolean trackCpu;
		private long lastCpuTime;
		
		/**
		 * When control thread decides to forcibly reduce
		 * thread population. It set blackMark for few threads
		 * which should be discontinued.
		 */
		private volatile boolean blackMark;
		
		@Override
		public void run() {
			originalThreadName = thread.getName();
			setThreadName(null);
			while(!terminated) {
				try {
					TaskUnit tu = pollTask();
					if (tu != null) {
						idleThreads.decrementAndGet();
						checkThreadDemand(false);
						execute(tu);
						// TODO 
						if (blackMark) {							
							break;
						}
						idleThreads.incrementAndGet();
						continue;
					}

					if (isOverQuota()) {
						break;
					}
					else {
						// waiting until somebody will trigger execution 
						scheduleSignal.acquire();
					}
					
				} catch (InterruptedException e) {
					if (terminated) {
						return;
					}
				}
			}
			dispose();
		}

		public float collectUsage() {
			if (currentTask == null) {
				// idle threads are counted against cap
				return 1f;
			}
			else {
				int taskNo = this.taskNo;
				if (lastTask != taskNo) {
					lastTask = taskNo;
					lastCheck = now();
					encounters = 1;
					trackCpu = false;
					return 1f;
				}
				else {
					float factor = 1;
					long now = now();
					encounters++;
					if (trackCpu && now > lastCheck) {
						long newCpu = getCpuTime(thread);
						long cpuDelta = newCpu - lastCpuTime;
						factor = (1f * cpuDelta) / (now - lastCheck);
						lastCpuTime = newCpu;
					}
					else if (encounters > 2) {
						trackCpu = true;
						lastCpuTime = getCpuTime(thread);
					}
					lastCheck = now;
					return factor;
				}
			}
		}
		
		private boolean isOverQuota() {
			if (idleThreads.get() > standByCap) {
				int r = idleThreads.decrementAndGet();
				if (r >= standByCap) {
					return true;
				}
				else {
					idleThreads.incrementAndGet();
				}
			}
			return false;
		}
		
		private void dispose() {
			// IMPORTANT idleThreads is already decreased in isOverQuota
			
			if (TRACE) {
				System.out.println("Disposing thread " + workerNo);
			}
			controlLock.lock();
			try {
				threads.remove(thread.getId());
			}
			finally {
				controlLock.unlock();
			}
		}
		
		private void execute(TaskUnit unit) {
			this.taskNo++;
			this.currentTask = unit;
			this.started = now();
			setThreadName(unit);
			if (terminated) {
				unit.abort();
			}
			else {
				unit.run();
			}
			this.currentTask = null;
			setThreadName(null);
		}

		private void setThreadName(TaskUnit unit) {
			String tname = name + ":worker-" + workerNo;
			if (unit == null) {
				tname = tname + " - idle";
			}
			else {
				try {
					tname = tname + " - " + clip(unit.task.toString(), 32);
				}
				catch(Throwable e) {					
					tname = tname + " - ???"; 
				}
			}
			thread.setName(tname);
		}

		private String clip(String string, int limit) {
			return string.length() > limit ? string.substring(0, limit) : string;
		}
	}

	private static ThreadMXBean THREADS_MBEAN = ManagementFactory.getThreadMXBean(); 
	
	private static long getCpuTime(Thread thread) {
		return THREADS_MBEAN.getThreadCpuTime(thread.getId());
	}
	
}
