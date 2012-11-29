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
package org.gridkit.vicluster;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.gridkit.vicluster.MassExec;
import org.gridkit.vicluster.ViGroup;
import org.junit.Test;

public class ViGroupTest {

	@Test
	public void test_group_task_exec() throws InterruptedException, ExecutionException {
		
		ViGroup group = new ViGroup();
		group.addNode(new DummyViNode());
		group.addNode(new DummyViNode());
		
		final AtomicInteger counter = new AtomicInteger();
		
		Runnable incTask = new Runnable() {
			@Override
			public void run() {
				counter.incrementAndGet();
			}
		};
		
		group.exec(incTask);
		
		Assert.assertEquals(2, counter.intValue());
		
		group.submit(incTask).get();

		Assert.assertEquals(4, counter.intValue());
	}

	@Test
	public void test_group_task_mass_exec() throws InterruptedException, ExecutionException {
		
		ViGroup group = new ViGroup();
		group.addNode(new DummyViNode());
		group.addNode(new DummyViNode());
		
		final AtomicInteger counter = new AtomicInteger();
		
		Runnable incTask = new Runnable() {
			@Override
			public void run() {
				counter.incrementAndGet();
			}
		};
		
		MassExec.waitAll(group.massSubmit(incTask));
		
		Assert.assertEquals(2, counter.intValue());		
	}
	
	public class CounterCallable implements Callable<Integer> {
		
		private AtomicInteger counter = new AtomicInteger();

		@Override
		public Integer call() throws Exception {
			return counter.incrementAndGet();
		}
	}
	
	
}
