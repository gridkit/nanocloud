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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;
import org.gridkit.nanocloud.interceptor.ViHookBuilder;
import org.gridkit.nanocloud.testutil.maven.MavenClasspathManager;
import org.gridkit.vicluster.ViGroup;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.telecontrol.isolate.IsolateAwareNodeProvider;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class IsolateFeatureTest {

	ViManager cloud = new ViManager(new IsolateAwareNodeProvider());
	
	private ViNode createIsolateViHost(String name) {
		return cloud.node(name);
	}
	
	@After
	public void cleanIsolates() {
		cloud.shutdown();
		cloud = new ViManager(new IsolateViNodeProvider());
	}

	@Test
	public void verify_isolated_static_with_void_callable() {
		
		ViNode viHost1 = createIsolateViHost("node-1");
		ViNode viHost2 = createIsolateViHost("node-2");
		
		ViGroup group = ViGroup.group(viHost1, viHost2);
		
		viHost1.exec(new VoidCallable() {
			@Override
			public void call() throws Exception {
				StaticVarHost.TEST_STATIC_VAR = "isolate 1";
			}
		});

		viHost2.exec(new VoidCallable() {
			@Override
			public void call() throws Exception {
				StaticVarHost.TEST_STATIC_VAR = "isolate 2";
			}
		});
		
		List<String> results = group.massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 1, isolate 2]", results.toString());
	}

	@Test
	public void verify_isolated_static_with_callable() {
		
		ViNode viHost1 = createIsolateViHost("node-1");
		ViNode viHost2 = createIsolateViHost("node-2");
		
		ViGroup group = ViGroup.group(viHost1, viHost2);
		
		viHost1.exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				StaticVarHost.TEST_STATIC_VAR = "isolate 1";
				return null;
			}
		});
		
		viHost2.exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				StaticVarHost.TEST_STATIC_VAR = "isolate 2";
				return null;
			}
		});
		
		List<String> results = group.massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 1, isolate 2]", results.toString());
	}

	@Test
	public void verify_isolated_static_with_runnable() {
		
		ViNode viHost1 = createIsolateViHost("node-1");
		ViNode viHost2 = createIsolateViHost("node-2");
		
		ViGroup group = ViGroup.group(viHost1, viHost2);
		
		viHost1.exec(new Runnable() {
			@Override
			public void run() {
				StaticVarHost.TEST_STATIC_VAR = "isolate 1";
			}
		});
		
		viHost2.exec(new Runnable() {
			@Override
			public void run() {
				StaticVarHost.TEST_STATIC_VAR = "isolate 2";
			}
		});
		
		List<String> results = group.massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 1, isolate 2]", results.toString());
	}
	
	@Test
	public void verify_class_exclusion() {
		
		ViNode viHost1 = createIsolateViHost("node-1");
		ViNode viHost2 = createIsolateViHost("node-2");
		
		ViGroup group = ViGroup.group(viHost1, viHost2);
		
		IsolateProps.at(group).shareClass(StaticVarHost.class);
		
		viHost1.exec(new Runnable() {
			@Override
			public void run() {
				StaticVarHost.TEST_STATIC_VAR = "isolate 1";
			}
		});
		
		viHost2.exec(new Runnable() {
			@Override
			public void run() {
				StaticVarHost.TEST_STATIC_VAR = "isolate 2";
			}
		});
		
		List<String> results = group.massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 2, isolate 2]", results.toString());
	}	
	
	@Test
	public void verify_property_isolation() throws Exception {
		
		ViNode node1 = createIsolateViHost("node-1");
		ViNode node2 = createIsolateViHost("node-2");

		node1.exec(new Runnable() {
			@Override
			public void run() {
				System.setProperty("local-prop", "Isolate1");				
			}
		});

		node2.exec(new Runnable() {
			@Override
			public void run() {
				System.setProperty("local-prop", "Isolate2");				
			}
		});

		node1.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("Isolate1", System.getProperty("local-prop"));				
			}
		});
		
		node2.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("Isolate2", System.getProperty("local-prop"));				
			}
		});		

		final String xxx = new String("Hallo from Isolate2");
		node2.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("Hallo from Isolate2", xxx);				
			}
		});		
		
		Assert.assertNull(System.getProperty("local-prop"));
	}
	
	@Test
	@Ignore("Stack traces are broken in generic cloud")
	public void verify_exec_stack_trace_locality() {

		ViNode node = createIsolateViHost("node-1");
		
		try {
			node.exec(new Runnable() {
				@Override
				public void run() {
					throw new IllegalArgumentException("test");
				}
			});
			Assert.assertFalse("Should throw an exception", true);
		}
		catch(IllegalArgumentException e) {
			e.printStackTrace();
			Assert.assertEquals(e.getMessage(), "test");
			assertLocalStackTrace(e);
		}
	}

	private void assertLocalStackTrace(IllegalArgumentException e) {
		Exception local = new Exception();
		int depth = local.getStackTrace().length - 2; // ignoring local and calling frame
		Assert.assertEquals(
				printStackTop(e.getStackTrace(),depth), 
				printStackTop(new Exception().getStackTrace(), depth)
		);
	}
	
	private static String printStackTop(StackTraceElement[] stack, int depth) {
		StringBuilder sb = new StringBuilder();
		int n = stack.length - depth;
		n = n < 0 ? 0 : n;
		for(int i = n; i != stack.length; ++i) {
			sb.append(stack[i]).append("\n");
		}
		return sb.toString();
	}
	
	// TODO expose export feature
	@Test @Ignore("Feature is missing")
	public void test_stack_trace2() {

		Isolate is1 = new Isolate("node-1", "com.tangosol", "org.gridkit");
		is1.start();
		
		try {
			Runnable r = is1.export(new Callable<Runnable>() {
				public Runnable call() {
					return 	new Runnable() {
						@Override
						public void run() {
							throw new IllegalArgumentException("test2");
						}
					};
				}
			});

			r.run();
			
			Assert.assertFalse("Should throw an exception", true);
		}
		catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
	}	
	
	@Test
	public void test_classpath_extention() throws IOException, URISyntaxException {
		
		ViNode node = createIsolateViHost("test-node");
		
		URL jar = getClass().getResource("/marker-override.jar");
		File jarFile = new File(jar.toURI());
		JvmProps.at(node).addClassPathElement(jarFile.getPath());
		
		node.exec(new Callable<Void>() {
			
			@Override
			public Void call() throws Exception {
				String marker = readMarkerFromResources();
				Assert.assertEquals("Marker from jar", marker);
				return null;
			}

		});
		
		Assert.assertEquals("Default marker", readMarkerFromResources());
	}

	private static String readMarkerFromResources() throws IOException {
		URL url = IsolateFeatureTest.class.getResource("/marker.txt");
		Assert.assertNotNull(url);
		BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
		String marker = r.readLine();
		r.close();
		return marker;
	}
	
	@Test(expected = NoClassDefFoundError.class)
	public void test_classpath_limiting() throws MalformedURLException, URISyntaxException {
		ViNode node = createIsolateViHost("test-node");
		
		MavenClasspathManager.removeArtifactVersion(node, "junit", "junit");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				// should throw NoClassDefFoundError because junit was removed from isolate classpath
				Assert.assertTrue(true);
			}
		});		
	}
	
	@Test
	public void test_annonimous_primitive_in_args() {
		
		ViNode node = createIsolateViHost("test_annonimous_primitive_in_args");
		
		final boolean fb = trueConst();
		final int fi = int_10();
		final double fd = double_10_1();
		
		node.exec(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				Assert.assertEquals("fb", true, fb);
				Assert.assertEquals("fi", 10, fi);
				Assert.assertEquals("fd", 10.1d, fd, 0d);
				return null;
			}			
		});
	}

	private double double_10_1() {
		return 10.1d;
	}

	private int int_10() {
		return 9 + 1;
	}

	private boolean trueConst() {
		return true & true;
	}	
	
	@Test
	public void test_instrumentation_return_value() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");
		
		ViNode node = createIsolateViHost("test_instrumentation_return_value");

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
		
		ViNode node = createIsolateViHost("test_instrumentation_expection_fallthrough");
		
		ViHookBuilder
		.newCallSiteHook(new LongReturnValueShifter(-111111))
		.onTypes(IsolateFeatureTest.class)
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
		
		ViNode node = createIsolateViHost("test_instrumentation_execution_prevention");
		
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
		
		ViNode node = createIsolateViHost("test_instrumentation_exception");
		
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
		.onTypes(IsolateFeatureTest.class)
		.onMethod("getSomething")
		.matchParams(key)
		.doReturn(value)
		.apply(node);
	}

	private void addErrorRule(ViNode node, Object key, Throwable e) {
		ViHookBuilder
		.newCallSiteHook()
		.onTypes(IsolateFeatureTest.class)
		.onMethod("getSomething")
		.matchParams(key)
		.doThrow(e)
		.apply(node);
	}
	
	@Test
	public void test_instrumentation_handler_staking() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");
		
		ViNode node = createIsolateViHost("test_instrumentation_exception");

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
