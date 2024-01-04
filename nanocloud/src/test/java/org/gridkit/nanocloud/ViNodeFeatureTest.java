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
import static org.gridkit.nanocloud.VX.CONSOLE;
import static org.gridkit.nanocloud.VX.HOOK;
import static org.gridkit.nanocloud.VX.PROCESS;
import static org.gridkit.nanocloud.VX.RUNTIME;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.assertj.core.api.Assertions;
import org.gridkit.nanocloud.agent.SampleAgent;
import org.gridkit.nanocloud.agent.SampleAgent2;
import org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf;
import org.gridkit.nanocloud.viengine.ProcessLifecycleListener;
import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public abstract class ViNodeFeatureTest {

    protected Cloud cloud;

    protected void assumeOutOfProcess() {
    }

    protected void assumeClassIsolation() {
    }

    protected void assumeInProcessIsolation() {
        Assume.assumeTrue(false);
    }

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void prepare() {
        System.getProperties().remove("local-prop");
    }

    @Before
    public abstract void initCloud();

    @After
    public void shutdownCloud() {
        cloud.shutdown();
    }

    public ViNode testNode() {
        return cloud.node(testName.getMethodName());
    }

    @Test
    public void verify_isolation_static_with_void_callable() {

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

    @Test
    public void verify_isolation_static_with_callable() {
        assumeClassIsolation();

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

    @Test
    public void verify_isolation_static_with_runnable() {
        assumeClassIsolation();

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

    @Test
    public void verify_classpath_class_sharing() {
        assumeClassIsolation();
        assumeInProcessIsolation();

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

        Assert.assertEquals("Host class is shared, so static field values should match", "[isolate 2, isolate 2]", results.toString());
    }

    @Test
    public void verify_isolation_system_properties() throws Exception {

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
    public void verify_execution_stack_trace_locality() {

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

    @Test
    public void verify_execution_transparent_proxy_stack_trace() {

        ViNode node = testNode();

        try {
            Runnable r = node.exec(new Callable<Runnable>() {
                @Override
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

    @Test
    public void verify_execution_transitive_transparent_proxy_stack_trace() {

        ViNode node = testNode();

        final RemoteRunnable explosive = new RemoteRunnable() {

            @Override
            public void run() {
                throw new IllegalArgumentException("test2");
            }
        };

        try {
            node.exec(new Callable<Void>() {
                @Override
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
            assertStackTraceContainsFrame(e, "[master] java.lang.Runnable.run(Remote call)");
            assertStackTraceContainsFrame(e, "[" + node + "] org.gridkit.zerormi.RemoteExecutor.exec(Remote call)");
        }
    }

    @Test
    public void verify_classpath_extention() throws IOException, URISyntaxException {
        assumeClassIsolation();

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

    @Test
    public void verify_classpath_limiting() throws MalformedURLException, URISyntaxException {
        assumeClassIsolation();

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

    @Test
    public void verify_classpath_dont_inherit_cp() {
        assumeClassIsolation();

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

    @Test
    public void verify_classpath_handle_NoDefClassFound(){
        assumeClassIsolation();
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

    @Test
    public void verify_classpath_handle_NoDefClassFound_on_return(){
        assumeClassIsolation();

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

    @Test
    public void verify_classpath_inherit_cp_true() throws IOException, URISyntaxException {
        assumeClassIsolation();

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

    @Test
    public void verify_classpath_inherit_cp_shallow() throws IOException, URISyntaxException {
        assumeClassIsolation();

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

    @Test
    public void verify_classpath_inherit_cp_default_true() {
        assumeClassIsolation();

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

    @Test
    public void verify_execution_annonimous_primitive_in_args() {

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

    @Test
    public void verify_runtime_process_new_env_variable() {
        assumeOutOfProcess();

        ViNode node = testNode();
        node.x(PROCESS).setEnv("TEST_VAR", "TEST");
        node.exec(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("TEST",System.getenv("TEST_VAR"));
            }
        });
    }

    @Test
    public void verify_runtime_process_env_variable_removal() {
        assumeOutOfProcess();

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

    @Test
    public void verify_jvm_single_arg_passing() {
        assumeOutOfProcess();

        ViNode node = testNode();
        node.x(PROCESS).addJvmArg("-DtestProp=TEST");
        node.exec(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("TEST",System.getProperty("testProp"));
            }
        });
    }

    @Test
    public void verify_jvm_multiple_args_passing() {
        assumeOutOfProcess();

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

    @Test
    public void verify_jvm_agent() throws Exception {
        assumeOutOfProcess();

        ViNode node = testNode();
        node.x(PROCESS).addAgent(packAgent(SampleAgent.class));
        node.exec(new Runnable() {
            @Override
            public void run() {
                Assert.assertNull(SampleAgent.options.get());
            }
        });
    }

    @Test
    public void verify_jvm_agent_with_options() throws Exception {
        assumeOutOfProcess();

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

    @Test
    public void verify_jvm_agent_multiple_agents() throws Exception {
        assumeOutOfProcess();

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

    @Test
    public void verify_jvm_invalid_arg_error() {
        assumeOutOfProcess();

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

    @Test
    public void verify_runtime_process_working_dir() throws IOException {
        assumeOutOfProcess();

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

    @Test
    public void verify_runtime_process_exit_code_is_available() throws Exception {
        assumeOutOfProcess();

        ViNode node = cloud.node(testName.getMethodName());

        // Schedule node crush after some time.
        node.exec(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(50);
                            Runtime.getRuntime().halt(42);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        Thread.sleep(500);

        Integer exitCode = node.x(RUNTIME).exitCodeFuture().get(15, TimeUnit.SECONDS);
        Assert.assertEquals((Object)42, exitCode);
    }

    @Test
    public void verify_runtime_process_exit_code_is_reported() {
        assumeOutOfProcess();

        ViNode node = cloud.node(testName.getMethodName());

        // Schedule node crush after some time.
        node.exec(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            Runtime.getRuntime().halt(42);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        try {
            Thread.sleep(2000); // halt timeout was 1000, after 2000 all processing should be done
            node.exec(new Runnable() {
                @Override
                public void run() {
                    System.out.println("ping");
                }
            });
            throw new AssertionError("Halt failed");
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            assertThat(writer.toString()).contains("exitCode=42");
        }
    }

    @Test
    public void verify_runtime_process_lifecycle_listener_receive_exit_code() throws Exception {
        assumeOutOfProcess();

        ViNode node = cloud.node(testName.getMethodName());

        ProcListener pll = new ProcListener();
        node.x(VX.PROCESS).addProcessListener(pll);

        // Schedule node crush after some time.
        node.exec(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(50);
                            Runtime.getRuntime().halt(42);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        long start = System.nanoTime();
        try {
            while(true) {
                node.exec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                        System.out.println("ping");
                    }
                });
                if (System.nanoTime() > start + TimeUnit.SECONDS.toNanos(5)) {
                    Assert.fail("Node lives too long");
                }
            }
        } catch (Exception e) {
            // execution failed;
        }

        pll.assertNodeStarted(node.toString());
        pll.waitNodeExitCode(node.toString(), 42);

    }

    @Test
    public void verify_runtime_process_lifecycle_listener_with_invalid_executable() throws Exception {
        assumeOutOfProcess();

        ViNode node = cloud.node(testName.getMethodName());

        ProcListener pll = new ProcListener();
        node.x(VX.PROCESS).addProcessListener(pll);
        // valid JVM to start tunneler
        node.setProp(SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC, "java");
        // broken JVM for node
        node.x(VX.PROCESS).setJavaExec("nosuchfile");

        try {
            node.touch();
            Assert.fail("Expected to fail");
        } catch (Exception e) {
            // ignore
        }

        pll.assertNodeNotStarted(node.toString());
        pll.assertExecFail(node.toString());
    }

    @Test
    public void verify_runtime_process_lifecycle_listener_with_invalid_arg() throws Exception {
        assumeOutOfProcess();

        ViNode node = cloud.node(testName.getMethodName());

        ProcListener pll = new ProcListener();
        node.x(VX.PROCESS).addProcessListener(pll);
        node.x(VX.PROCESS).addJvmArg("-XX:+InvalidOption");

        try {
            node.touch();
            Assert.fail("Expected to fail");
        } catch (Exception e) {
            // ignore
        }

        pll.assertNodeStarted(node.toString());
        pll.waitNodeExitCode(node.toString(), 1);
    }

    @Test
    public void verify_console_capture_std_out() throws Exception {

        ViNode node = cloud.node(testName.getMethodName());

        StringWriter writer = new StringWriter();
        node.x(CONSOLE).bindOut(writer);

        node.exec(new Runnable() {

            @Override
            public void run() {
                System.out.println("Test message");
                System.err.flush();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        });

        node.x(CONSOLE).flush();

        Assertions.assertThat(writer.toString()).contains("Test message");
        Assertions.assertThat(writer.toString()).doesNotContain(testName.getMethodName());
    }

    @Test
    public void verify_console_capture_std_err() throws Exception {

        ViNode node = cloud.node(testName.getMethodName());

        StringWriter writer = new StringWriter();
        node.x(CONSOLE).bindErr(writer);

        node.exec(new Runnable() {

            @Override
            public void run() {
                System.err.println("Test message");
                System.err.flush();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        });

        node.x(CONSOLE).flush();

        Assertions.assertThat(writer.toString()).contains("Test message");
        Assertions.assertThat(writer.toString()).doesNotContain(testName.getMethodName());
    }

    //@Test TODO std in manipulation
    public void verify_console_inject_std_in() throws Exception {

//        ViNode node = cloud.node(testName.getMethodName());
//
//        StringWriter writer = new StringWriter();
    }

    @Test
    public void verify_hook_on_startup() throws Exception {

        ViNode node = cloud.node(testName.getMethodName());

        RemoteFutureBox<Void> box = new RemoteFutureBox<Void>();
        final RemoteBox<Void> rbox = box;

        node.x(HOOK).addStartupHook(new Runnable() {

            @Override
            public void run() {
                rbox.setData(null);
            }
        });

        node.touch();

        Assertions.assertThat(box.isDone()).isTrue();
    }

    @Test
    public void verify_hook_failure_on_startup() throws Exception {

        ViNode node = cloud.node(testName.getMethodName());

        final FutureBox<Void> box = new FutureBox<Void>();

        node.x(HOOK).addStartupHook(new Runnable() {
            // will not be able to serialize this
            @Override
            public void run() {
                box.setData(null);
            }
        });

        try {
            node.touch();
        } catch (Exception e) {
            e.printStackTrace();
            assertStackTraceContainsText(e, "java.io.NotSerializableException: org.gridkit.util.concurrent.FutureBox");
        }
    }

    @Test
    public void verify_hook_on_shutdown() throws Exception {

        ViNode node = cloud.node(testName.getMethodName());

        RemoteFutureBox<Void> box = new RemoteFutureBox<Void>();
        final RemoteBox<Void> rbox = box;

        node.x(HOOK).addShutdownHook(new Runnable() {

            @Override
            public void run() {
                rbox.setData(null);
            }
        });

        node.touch();

        node.shutdown();

        Assertions.assertThat(box.isDone()).isTrue();
    }

    @Test
    public void verify_hook_on_post_shutdown() throws Exception {

        ViNode node = cloud.node(testName.getMethodName());

        final FutureBox<Void> box = new FutureBox<Void>();

        // TODO allow post shutdown hooks on active nodes
        node.x(HOOK).addPostShutdownHook(new Runnable() {

            @Override
            public void run() {
                box.setData(null);
            }
        });

        node.touch();

        node.shutdown();

        Assertions.assertThat(box.isDone()).isTrue();
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

    private void assertStackTraceContainsFrame(Exception e, String line) {
        for(StackTraceElement ee: e.getStackTrace()) {
            if (ee.toString().contains(line)) {
                return;
            }
        }
        Assert.fail("Line: " + line + "\n is not found in stack traces\n" + printStackTop(e.getStackTrace(), e.getStackTrace().length));
    }

    private void assertStackTraceContainsText(Exception e, String line) {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        if (!w.toString().contains(line)) {
            Assert.fail("Line: " + line + "\n is not found in stack traces\n" + w.toString());
        }
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

    private static class ProcListener implements ProcessLifecycleListener {

        List<ExecInfo> execInfos = new ArrayList<ProcessLifecycleListener.ExecInfo>();
        List<ExecFailedInfo> execFailedInfos = new ArrayList<ProcessLifecycleListener.ExecFailedInfo>();
        List<TerminationInfo> termInfos = new ArrayList<ProcessLifecycleListener.TerminationInfo>();

        @Override
        public synchronized void processStarted(String nodeName, ExecInfo launchInfo) {
            this.execInfos.add(launchInfo);
        }

        @Override
        public synchronized void processExecFailed(String nodeName, ExecFailedInfo launchInfo) {
            this.execFailedInfos.add(launchInfo);

        }

        @Override
        public synchronized void processTerminated(String nodeName, TerminationInfo termInfo) {
            this.termInfos.add(termInfo);
        }

        public synchronized void assertNodeStarted(String nodename) {
            for (ExecInfo ei: execInfos) {
                if (ei.getNodeName().equals(nodename)) {
                    return;
                }
            }
            Assert.fail("No launch event for [" + nodename + "]");
        }

        public synchronized void assertNodeNotStarted(String nodename) {
            for (ExecInfo ei: execInfos) {
                if (ei.getNodeName().equals(nodename)) {
                    Assert.fail("Should be no start event for [" + nodename + "], but one is found");
                }
            }
        }

        public synchronized void assertExecFail(String nodename) {
            for (ExecFailedInfo ei: execFailedInfos) {
                if (ei.getNodeName().equals(nodename)) {
                    System.err.println("Exec failed on [" + nodename + "] - " + ei.getError());
                    return;
                }
            }
            Assert.fail("No exec fail event for [" + nodename + "]");
        }

        public void waitNodeExitCode(String nodename, int exitCode) {
            long start = System.nanoTime();
            while(true) {
                synchronized(this) {
                    for (TerminationInfo ei: termInfos) {
                        if (ei.getNodeName().equals(nodename)) {
                            Assert.assertEquals("exitCode", exitCode, ei.getExitCode());
                            return;
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
                if (System.nanoTime() > start + TimeUnit.SECONDS.toNanos(5)) {
                    break;
                }
            }
            Assert.fail("No termination event for [" + nodename + "]");
        }
    }

    private interface RemoteBox<V> extends Box<V>, Remote {

    }

    public static class RemoteFutureBox<V> implements RemoteBox<V> {

        private FutureBox<V> box = new FutureBox<V>();

        public V get() throws InterruptedException, ExecutionException {
            return box.get();
        }

        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return box.get(timeout, unit);
        }

        @Override
        public void setData(V data) {
            box.setData(data);
        }

        @Override
        public void setError(Throwable e) {
            box.setError(e);
        }

        public void setErrorIfWaiting(Throwable e) {
            box.setErrorIfWaiting(e);
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return box.cancel(mayInterruptIfRunning);
        }

        public boolean isCancelled() {
            return box.isCancelled();
        }

        public boolean isDone() {
            return box.isDone();
        }
    }
}
