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
	public void canceled() {
		task.canceled();
	}
}
