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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.TaskService.Task;
import org.junit.Test;

public class TaskServiceTest {

	SensibleTaskService taskService = new SensibleTaskService("TestPool");
	
	@Test
	public void verify_poll_task() throws InterruptedException {
		SimplePollTask task1 = new SimplePollTask("task1", taskService);
		SimplePollTask task2 = new SimplePollTask("task2", taskService);
		SimplePollTask task3 = new SimplePollTask("task3", taskService);
		SimplePollTask task4 = new SimplePollTask("task4", taskService);
		SimplePollTask task5 = new SimplePollTask("task5", taskService);
		SimplePollTask task6 = new SimplePollTask("task6", taskService);
		taskService.schedule(task1, 1, TimeUnit.DAYS);
		taskService.schedule(task2);
		taskService.schedule(task3);
		taskService.schedule(task4);
		taskService.schedule(task5);
		taskService.schedule(task6);
		
		taskService.schedule(new HungTask());
		taskService.schedule(new HungTask());
		taskService.schedule(new HungTask());
		taskService.schedule(new HungTask());
		taskService.schedule(new HungTask());
		taskService.schedule(new HungTask());
		
		Thread.sleep(30000);
		System.out.println("Stoping service");
		taskService.shutdown();
		Thread.sleep(30000);
	}
	
	public static class HungTask implements Task {
		
		private Semaphore lock = new Semaphore(0);		

		@Override
		public void run() {
			lock.acquireUninterruptibly();
			System.out.println("Lock passed");
		}

		@Override
		public void interrupt(Thread taskThread) {
			lock.release();
		}

		@Override
		public void canceled() {
			lock.release();
		}
	}
	
	public static class SimplePollTask implements Task {
		
		@SuppressWarnings("unused")
		private String name;
		private TaskService service;
		private long start;
		private int pollCount;
		
		public SimplePollTask(String name, TaskService service) {
			this.name = name;
			this.service = service;
			this.start = System.nanoTime();
		}

		@Override
		public void run() {
			++pollCount;
			service.schedule(this, 10, TimeUnit.MILLISECONDS);
		}

		@Override
		public void interrupt(Thread taskThread) {
			// ignore			
		}

		@Override
		public void canceled() {
			long time = System.nanoTime();
			if (pollCount > 0) {
				long interval = (time - start) / pollCount;
				System.out.println("Task stopped, " + pollCount + " invocations. Average interval: " + TimeUnit.NANOSECONDS.toMillis(interval));
			}
			else {
				System.out.println("Task stopped, 0 invocations");
			}
		}
	}
	
}
