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
package org.gridkit.vicluster.isolate;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.interceptor.ViHookBuilder;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InstrumentationFeatureTest {

	protected Cloud cloud;
	
	@Before
	public void initCloud() {
		cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setIsolateType();
	}
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}

	protected ViNode node(String name) {
		return cloud.node(name);
	}
	
	@Test
	public void test_instrumentation_return_value() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");
		
		ViNode node = node("test_instrumentation_return_value");

		ViHookBuilder
			.newCallSiteHook(new LongReturnValueShifter(-111111))
			.onTypes(System.class)
			.onMethod("currentTimeMillis")
			.apply(node);
		
		long time = System.currentTimeMillis();
		
		long itime = node.exec(new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				return System.currentTimeMillis();
			}
		});
		
		Assert.assertTrue("Time expected to be shifted back", itime < time);
	}

	@Test
	public void test_instrumentation_expection_fallthrough() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");
		
		ViNode node = node("test_instrumentation_expection_fallthrough");
		
		ViHookBuilder
		.newCallSiteHook(new LongReturnValueShifter(-111111))
		.onTypes(InstrumentationFeatureTest.class)
		.onMethod("explode")
		.apply(node);
		
		node.exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					explode("test");
					Assert.fail("Exception expected");
				}
				catch(IllegalStateException e) {
					Assert.assertEquals("test", e.getMessage());
				}
				return null;
			}
		});
	}

	private static long explode(String msg) {
		throw new IllegalStateException(msg);
	}
	
	@Test
	public void test_instrumentation_execution_prevention() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");
		
		ViNode node = node("test_instrumentation_execution_prevention");
		
		ViHookBuilder
		.newCallSiteHook()
		.onTypes(System.class)
		.onMethod("exit")
		.doReturn(null)
		.apply(node);
		
		node.exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				System.exit(0);
				return null;
			}
		});
	}

	@Test(expected=IllegalStateException.class)
	public void test_instrumentation_exception() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");
		
		ViNode node = node("test_instrumentation_exception");
		
		ViHookBuilder
		.newCallSiteHook()
		.onTypes(System.class)
		.onMethod("exit")
		.doThrow(new IllegalStateException("Ka-Boom"))
		.apply(node);
		
		node.exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				System.exit(0);
				return null;
			}
		});
	}

	private void addValueRule(ViNode node, Object key, Object value) {
		ViHookBuilder
		.newCallSiteHook()
		.onTypes(InstrumentationFeatureTest.class)
		.onMethod("getSomething")
		.matchParams(key)
		.doReturn(value)
		.apply(node);
	}

	private void addErrorRule(ViNode node, Object key, Throwable e) {
		ViHookBuilder
		.newCallSiteHook()
		.onTypes(InstrumentationFeatureTest.class)
		.onMethod("getSomething")
		.matchParams(key)
		.doThrow(e)
		.apply(node);
	}
	
	@Test
	public void test_instrumentation_handler_staking() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");
		
		ViNode node = node("test_instrumentation_exception");

		addValueRule(node, "A", "a");
		addValueRule(node, "B", "b");
		addValueRule(node, "B", "bb");
		addErrorRule(node, "X", new IllegalStateException("Just for fun"));
		
		node.exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {

				Assert.assertEquals("a", getSomething("A"));
				Assert.assertEquals("bb", getSomething("B"));
				Assert.assertNull(getSomething("C"));
				
				try {
					getSomething("X");
					Assert.fail();
				}
				catch(IllegalStateException e) {
					Assert.assertEquals("Just for fun", e.getMessage());
				}
				
				return null;
			}
		});
	}

	private static Object getSomething(Object key) {
		return null;
	}
	
	@SuppressWarnings("serial")
	public static class LongReturnValueShifter implements Interceptor, Serializable {

		private long shift;
		
		public LongReturnValueShifter(long shift) {
			this.shift = shift;
		}

		@Override
		public void handle(Interception hook) {
			try {
				Long value = (Long) hook.call();
				hook.setResult(value + shift);
			} catch (ExecutionException e) {
				// fall though
			}
		}
	}
}
