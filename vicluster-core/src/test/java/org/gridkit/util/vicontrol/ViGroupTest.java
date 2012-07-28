package org.gridkit.util.vicontrol;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

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
