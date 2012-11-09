package org.gridkit.util.concurrent;

import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.TaskService.Task;

public class RecuringTask implements Task {
	
	public static void start(TaskService service, Task task, long period, TimeUnit tu) {
		RecuringTask rtask = new RecuringTask(service, task, period, tu);
		service.schedule(rtask);
	}
	
	private static long ANCHOR = System.nanoTime();
	
	private static long now() {
		return System.nanoTime() - ANCHOR;
	}
	
	private final TaskService service;
	private final long period;
	private final Task task;
	private long lastSchedule = -1;
	
	public RecuringTask(TaskService service, Task task, long period, TimeUnit tu) {
		this.service = service;
		this.task = task;
		this.period = tu.toNanos(period);
	}

	@Override
	public void run() {		
		long nextSchedule = (lastSchedule == -1 ? now() : lastSchedule) + period;
		lastSchedule = nextSchedule;
		try {
			task.run();
		}
		finally {
			schedule(lastSchedule);
		}
	}

	private void schedule(long schedule) {
		long delay = schedule - now();
		if (delay <= 0) {
			service.schedule(this);
		}
		else {
			service.schedule(this, delay, TimeUnit.NANOSECONDS);
		}
	}

	@Override
	public void interrupt(Thread taskThread) {
		task.interrupt(taskThread);
	}

	@Override
	public void cancled() {
		task.cancled();
	}
}
