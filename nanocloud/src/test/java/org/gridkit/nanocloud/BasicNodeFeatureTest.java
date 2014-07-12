/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.VX.CLASSPATH;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public abstract class BasicNodeFeatureTest {

	protected Cloud cloud;
	
	@Before
	public abstract void initCloud();
	
	@After
	public void shutdownCloud() {
		cloud.shutdown();
	}

	public void verify_isolated_static_with_void_callable() {
		
		ViNode viHost1 = cloud.node("node-1");
		ViNode viHost2 = cloud.node("node-2");
		
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
		
		List<String> results = cloud.nodes("node-1", "node-2").massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 1, isolate 2]", results.toString());
	}

	public void verify_isolated_static_with_callable() {
		
		ViNode viHost1 = cloud.node("node-1");
		ViNode viHost2 = cloud.node("node-2");
		
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
		
		List<String> results = cloud.nodes("node-1", "node-2").massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 1, isolate 2]", results.toString());
	}

	public void verify_isolated_static_with_runnable() {
		
		ViNode viHost1 = cloud.node("node-1");
		ViNode viHost2 = cloud.node("node-2");
		
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
		
		List<String> results = cloud.nodes("node-1", "node-2").massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 1, isolate 2]", results.toString());
	}
	
	public void verify_class_exclusion() {
		
		ViNode viHost1 = cloud.node("node-1");
		ViNode viHost2 = cloud.node("node-2");
		
		IsolateProps.at(cloud.node("**")).shareClass(StaticVarHost.class);
		
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
		
		List<String> results = cloud.nodes("node-1", "node-2").massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return StaticVarHost.TEST_STATIC_VAR;
			}
		});
		
		Assert.assertEquals("Static variable should be different is different isolartes", "[isolate 2, isolate 2]", results.toString());
	}	
	
	public void verify_property_isolation() throws Exception {
		
		ViNode node1 = cloud.node("node-1");
		ViNode node2 = cloud.node("node-2");

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
	public void verify_exec_stack_trace_locality() {

		ViNode node = cloud.node("node-1");
		
		try {
			node.exec(new Runnable() {
				@Override
				public void run() {
					throw new IllegalArgumentException("test");
				}
			});
			Assert.fail("Should throw an exception");
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
				printStackTop(new Exception().getStackTrace(), depth), 
				printStackTop(e.getStackTrace(),depth)
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
	
	@Test
	public void test_stack_trace2() {

		Isolate is1 = new Isolate("node-1", "com.tangosol", "org.gridkit");
		is1.start();

        int lineNumberBeforeRun = 0;
        int lineNumberAfterRun = 0;
		
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

            lineNumberBeforeRun = new Throwable().getStackTrace()[0].getLineNumber();
            try {
                r.run();
            } finally {
                lineNumberAfterRun = new Throwable().getStackTrace()[0].getLineNumber();
            }

            Assert.fail("Should throw an exception");
		}
		catch(IllegalArgumentException e) {
			e.printStackTrace();
            StackTraceElement elementFromCurrentMethod = null;
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                if (stackTraceElement.getClassName().equals(BasicNodeFeatureTest.class.getName()) &&
                        stackTraceElement.getMethodName().equals("test_stack_trace2")){
                    elementFromCurrentMethod = stackTraceElement;
                    break;
                }
            }
            assertNotNull("stack trace should contains client-side method", elementFromCurrentMethod);
            final int lineNumber = elementFromCurrentMethod.getLineNumber();
            assertTrue("stack trace should contains line where method was invoked", lineNumber > lineNumberBeforeRun && lineNumber < lineNumberAfterRun);
        }
	}	
	
	public void test_classpath_extention() throws IOException, URISyntaxException {
		
		ViNode node = cloud.node("test-node");
		
		URL jar = getClass().getResource("/marker-override.jar");
		File path = new File(jar.toURI());
		node.x(CLASSPATH).add(path.getAbsolutePath());
		
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
        URL url = IsolateNodeFeatureTest.class.getResource("/marker.txt");
		assertNotNull(url);
		BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
		String marker = r.readLine();
		r.close();
		return marker;
	}
	
	@Test(expected = NoClassDefFoundError.class)
	public void test_classpath_limiting() throws MalformedURLException, URISyntaxException {
		ViNode node = cloud.node("test-node");
		
		URL url = getClass().getResource("/org/junit/Assert.class");
		assertNotNull(url);
		
		String jarUrl = url.toString();
		jarUrl = jarUrl.substring(0, jarUrl.lastIndexOf('!'));
		jarUrl = jarUrl.substring("jar:".length());
		node.x(CLASSPATH).remove(new File(new URI(jarUrl)).getAbsolutePath());
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				// should throw NoClassDefFoundError because junit was removed from isolate classpath
				Assert.assertTrue(true);
			}
		});		
	}
	
	public void test_annonimous_primitive_in_args() {
		
		ViNode node = cloud.node("test_annonimous_primitive_in_args");
		
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
}
