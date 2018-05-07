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

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridkit.nanocloud.VX.CLASSPATH;
import static org.gridkit.nanocloud.VX.PROCESS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.gridkit.nanocloud.agent.SampleAgent;
import org.gridkit.nanocloud.agent.SampleAgent2;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class ViNodeFeatureTest {

	protected Cloud cloud;

	@Rule
	public TestName testName = new TestName();
	
	@Before
	public abstract void initCloud();
	
	@After
	public void shutdownCloud() {
		cloud.shutdown();
	}
	
	public ViNode testNode() {
	    return cloud.node(testName.getMethodName());
	}
	
    public void verify_isolated_static_with_void_callable() {
		
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
	
	public void verify_exec_stack_trace_locality() {

		ViNode node = testNode();
		
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

    public void verify_transparent_proxy_stack_trace() {

        ViNode node = testNode();
        
        try {
            Runnable r = node.exec(new Callable<Runnable>() {
                public Runnable call() {
                    return new RemoteRunnable() {
                        
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
            assertLocalStackTrace(e);
        }
    }   

    public void verify_transitive_transparent_proxy_stack_trace() {

        ViNode node = testNode();
        
        final RemoteRunnable explosive = new RemoteRunnable() {
            
            @Override
            public void run() {
                throw new IllegalArgumentException("test2");
            }
        };
        
        try {
            node.exec(new Callable<Void>() {
                public Void call() {
                    explosive.run();
                    return null;
                }
            });

            Assert.assertFalse("Should throw an exception", true);
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
            assertLocalStackTrace(e);
            assertStackTraceContains(e, "[master] java.lang.Runnable.run(Remote call)");
            assertStackTraceContains(e, "[" + node + "] org.gridkit.zerormi.RemoteExecutor.exec(Remote call)");
        }
    }       
    
	public void test_classpath_extention() throws IOException, URISyntaxException {
		
		ViNode node1 = cloud.node(testName.getMethodName()+"_1");
		ViNode node2 = cloud.node(testName.getMethodName()+"_2");
		ViNode node3 = cloud.node(testName.getMethodName()+"_3");

		node1.x(CLASSPATH).add(getClass().getResource("/marker-override.jar"));
		node1.x(CLASSPATH).add(getClass().getResource("/marker-override2.jar"));

		node2.x(CLASSPATH).add(getClass().getResource("/marker-override2.jar"));
		node2.x(CLASSPATH).add(getClass().getResource("/marker-override.jar"));

		node3.x(CLASSPATH).add(getClass().getResource("/marker-override2.jar"));
		node3.x(CLASSPATH).remove(getClass().getResource("/marker-override2.jar"));

//		node3.x(VX.PROCESS).addJvmArg("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");

		node1.exec(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				String marker = readMarkerFromResources();
				Assert.assertEquals("Marker from jar", marker);
				return null;
			}

		});

		node2.exec(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				String marker = readMarkerFromResources();
				Assert.assertEquals("Marker from jar 2", marker);
				return null;
			}

		});

		node3.exec(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				String marker = readMarkerFromResources();
				Assert.assertEquals("Default marker", marker);
				return null;
			}

		});
		
		Assert.assertEquals("Default marker", readMarkerFromResources());
	}

	public void test_classpath_limiting() throws MalformedURLException, URISyntaxException {
    	
	    ViNode node = testNode();
    	
    	URL url = getClass().getResource("/org/junit/Assert.class");
    	Assert.assertNotNull(url);
    	
    	String jarUrl = url.toString();
    	jarUrl = jarUrl.substring(0, jarUrl.lastIndexOf('!'));
    	jarUrl = jarUrl.substring("jar:".length());
    	node.x(CLASSPATH).remove(new File(new URI(jarUrl)).getAbsolutePath());
    
    	try {
    		node.exec(new Runnable() {
    			@Override
    			public void run() {
    				// should throw NoClassDefFoundError because junit was removed from isolate classpath
    				Assert.assertTrue(true);
    			}
    		});
    		Assert.fail("Exception is expected");
    	}
    	catch(Error e) {
    	    assertThat(e).isInstanceOf(NoClassDefFoundError.class);
	}
    }

    public void test_dont_inherit_cp() {

        ViNode node = testNode();

        node.x(CLASSPATH).inheritClasspath(false);

        try {
            node.exec(new Runnable() {
                @Override
                public void run() {
                    // should throw NoClassDefFoundError because junit should not inherited
                    Assert.assertTrue(true);
                }
            });
            Assert.fail("Exception is expected");
        }
        catch(Error e) {
            assertThat(e).isInstanceOf(NoClassDefFoundError.class);
        }
	}

	public void test_handle_NoDefClassFound(){
		ViNode node = testNode();

		node.x(CLASSPATH).inheritClasspath(false);
		
		try {
			node.exec(new Runnable() {
				@SuppressWarnings("unused")
                Assert anAssert = new Assert(){}; // NoClassDefFoundError during deserialization

				@Override
				public void run() {
				}
			});
			Assert.fail("Exception is expected");
		}
		catch(Exception e) {
			assertThat(e).isInstanceOf(RemoteException.class);
			assertThat(e.getCause()).isInstanceOf(NoClassDefFoundError.class);
		}
		
		// Verify that node is still live
		node.exec(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("ping");
            }
        });
	}

	public void test_handle_NoDefClassFound_on_return(){
	    ViNode node = testNode();
	    
	    node.x(CLASSPATH).inheritClasspath(false);

	    final Callable<Runnable> factory = new RemoteCallable<Runnable>() {

            @Override
            public Runnable call() throws Exception {
                return new Runnable() {
                    @SuppressWarnings("unused")
                    Assert anAssert = new Assert(){}; // NoClassDefFoundError during deserialization

                    @Override
                    public void run() {
                    }
                };
            }	        
	    };
	    
	    try {
	        node.exec(new Callable<Void>() {
	            @Override
	            public Void call() throws Exception {
	                factory.call().run();
	                return null;
	            }
	        });
	        Assert.fail("Exception is expected");
	    }
	    catch(Exception e) {
	        assertThat(e).isInstanceOf(RemoteException.class);
	        assertThat(e.getCause()).isInstanceOf(NoClassDefFoundError.class);
	    }
	    
	    // Verify that node is still live
	    node.exec(new Runnable() {
	        
	        @Override
	        public void run() {
	            System.out.println("ping");
	        }
	    });
	}

	public void test_inherit_cp_true() throws IOException, URISyntaxException {

        ViNode node = testNode();

        node.x(CLASSPATH).inheritClasspath(true);

        node.exec(new Runnable() {
            @Override
            public void run() {
                // should NOT throw NoClassDefFoundError because junit should be inherited
                Assert.assertTrue(true);
            }
        });
	}

	public void test_inherit_cp_shallow() throws IOException, URISyntaxException {
		
		ViNode node = testNode();
		
		node.x(CLASSPATH).inheritClasspath(true);
		node.x(CLASSPATH).useShallowClasspath(true);
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				// should NOT throw NoClassDefFoundError because junit should be inherited
				Assert.assertTrue(true);
			}
		});
	}

	public void test_inherit_cp_default_true() {

        ViNode node = testNode();

        //this is by default: node.x(CLASSPATH).inheritClasspath(true);

        node.exec(new Runnable() {
            @Override
            public void run() {
                // should NOT throw NoClassDefFoundError because junit should be inherited
                Assert.assertTrue(true);
            }
        });
	}

	public void test_annonimous_primitive_in_args() {

        ViNode node = testNode();
		
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

    public void verify_new_env_variable() {
        
        ViNode node = testNode();
        node.x(PROCESS).setEnv("TEST_VAR", "TEST");
        node.exec(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("TEST",System.getenv("TEST_VAR"));
            }
        });
    }

    public void verify_env_variable_removal() {
        
        ViNode node = testNode();
        node.x(PROCESS).setEnv("HOME", null);
        node.x(PROCESS).setEnv("HOMEPATH", null);
        node.exec(new Runnable() {
            @Override
            public void run() {             
                Assert.assertFalse("HOME expected to be empty", System.getenv().containsKey("HOME"));
                Assert.assertFalse("HOMEPATH expected to be empty", System.getenv().containsKey("HOMEPATH"));
            }
        });
    }
    
    public void verify_jvm_single_arg_passing() {
        
        ViNode node = testNode();
        node.x(PROCESS).addJvmArg("-DtestProp=TEST");
        node.exec(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("TEST",System.getProperty("testProp"));
            }
        });
    }

    public void verify_jvm_multiple_args_passing() {
        
        ViNode node = testNode();
        node.x(PROCESS).addJvmArg("-DtestProp=TEST");
        node.x(PROCESS).addJvmArgs("-DtestProp1=A", "-DtestProp2=B");
        node.exec(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("TEST",System.getProperty("testProp"));
                Assert.assertEquals("A",System.getProperty("testProp1"));
                Assert.assertEquals("B",System.getProperty("testProp2"));
            }
        });
    }

	public void verify_jvm_agent() throws Exception {
		ViNode node = testNode();
		node.x(PROCESS).addAgent(packAgent(SampleAgent.class));
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertNull(SampleAgent.options.get());
			}
		});
	}

	public void verify_jvm_agent_with_options() throws Exception {
		ViNode node = testNode();
		final String options = "my-super-options=abc";
		node.x(PROCESS).addAgent(packAgent(SampleAgent.class), options);
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals(options, SampleAgent.options.get());
			}
		});
	}

	public void verify_jvm_agent_multiple_agents() throws Exception {
		ViNode node = testNode();
		final String options1 = "my-super-options=abc";
		final String options2 = "my-super-options=bcd";

		node.x(PROCESS).addAgent(packAgent(SampleAgent.class), options1);
		node.x(PROCESS).addAgent(packAgent(SampleAgent2.class), options2);
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals(options1, SampleAgent.options.get());
				Assert.assertEquals(options2, SampleAgent2.options.get());
			}
		});
	}

	public void verify_jvm_invalid_arg_error() {
        
        ViNode node = testNode();
        JvmProps.addJvmArg(node, "-XX:+InvalidOption");

        try {
            node.exec(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Ping");
                }
            });
            Assert.fail("Exception expected");
        }
        catch(Throwable e) {
            e.printStackTrace();
            // expected
        }
    }   
    
    public void verify_slave_working_dir() throws IOException {
        
        ViNode nodeB = cloud.node(testName.getMethodName() + ".base");
        ViNode nodeC = cloud.node(testName.getMethodName() + ".child");
        nodeC.x(PROCESS).setWorkDir("target");

        final File base = nodeB.exec(new Callable<File>() {
            @Override
            public File call() throws Exception {
                File wd = new File(".").getCanonicalFile();
                File ndir = new File(wd, "target");
                ndir.mkdirs();
                System.err.println("Create directory: " + ndir.getAbsolutePath());
                return wd;
            }
        });
        nodeC.exec(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                File wd = new File(".").getCanonicalFile();
                System.err.println("Working directory: " + wd.getAbsolutePath());
                Assert.assertEquals(new File(base, "target").getCanonicalFile(), wd);
                return null;
            }
        });
    }
    
	private static String readMarkerFromResources() throws IOException {
    	URL url = IsolateNodeFeatureTest.class.getResource("/marker.txt");
    	Assert.assertNotNull(url);
    	BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
    	String marker = r.readLine();
    	r.close();
    	return marker;
    }

    private void assertLocalStackTrace(Exception e) {
    	Exception local = new Exception();
    	int depth = local.getStackTrace().length - 2; // ignoring local and calling frame
    	Assert.assertEquals(
    			printStackTop(new Exception().getStackTrace(), depth), 
    			printStackTop(e.getStackTrace(),depth)
    	);
    }

    private void assertStackTraceContains(Exception e, String line) {
        for(StackTraceElement ee: e.getStackTrace()) {
            if (ee.toString().contains(line)) {
                return;
            }
        }
        Assert.fail("Line: " + line + "\n is not found in stack traces\n" + printStackTop(e.getStackTrace(), e.getStackTrace().length));
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

    private double double_10_1() {
		return 10.1d;
	}

	private int int_10() {
		return 9 + 1;
	}

	private boolean trueConst() {
		return true & true;
	}
	
    public interface RemoteRunnable extends Runnable, Remote {
        
    }
    
    public interface RemoteCallable<T> extends Callable<T>, Remote {
        
    }

	private File packAgent(Class<?> agentClass) throws Exception {
		File agentJar = File.createTempFile("agent", ".jar");

		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(new Attributes.Name("PreMain-Class"), agentClass.getName());

		ZipOutputStream jarOut = new JarOutputStream(new FileOutputStream(agentJar), manifest);

		String path = agentClass.getName().replace('.', '/') + ".class";
		InputStream classStream = this.getClass().getClassLoader().getResourceAsStream(path);

		ZipEntry classEntry = new ZipEntry(path);
		jarOut.putNextEntry(classEntry);
		copyNoClose(classStream, jarOut);
		jarOut.closeEntry();

		jarOut.closeEntry();
		jarOut.close();
		return agentJar;
	}

	public static void copyNoClose(InputStream in, OutputStream out) throws IOException {
		boolean doClose = true;
		try {
			byte[] buf = new byte[1 << 12];
			while(true) {
				int n = in.read(buf);
				if(n >= 0) {
					out.write(buf, 0, n);
				}
				else {
					break;
				}
			}
			doClose = false;

		} finally {
			if (doClose) {
				// close if there were exception thrown
				try {
					in.close();
				}
				catch(Exception e) {
					// ignore
				}
			}
		}
	}	
}
