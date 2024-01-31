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

import static org.gridkit.nanocloud.VX.ISOLATE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.Remote;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.assertj.core.api.Assertions;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.Nanocloud;
import org.gridkit.nanocloud.VX;
import org.gridkit.nanocloud.ViNode;
import org.junit.Assert;
import org.junit.Test;

public class IsolateFeatureTest {

    Cloud cloud = Nanocloud.createCloud();
    {
        cloud.x(VX.ISOLATE);
    }

    @Test
    public void verify_isolated_static_with_void_callable() {

        ViNode viHost1 = cloud.node("node-1");
        ViNode viHost2 = cloud.node("node-2");

        ViNode group = Cloud.multiNode(viHost1, viHost2);

        viHost1.exec(() -> {
            StaticVarHost.TEST_STATIC_VAR = "isolate 1";
        });

        viHost2.exec(() -> {
            StaticVarHost.TEST_STATIC_VAR = "isolate 2";
        });

        Collection<String> results = group.massCalc(() -> {
            return StaticVarHost.TEST_STATIC_VAR;
        }).all();

        Assertions.assertThat(results).contains("isolate 1", "isolate 2");
    }

    @Test
    public void verify_isolated_static_with_callable() {

        ViNode viHost1 = cloud.node("node-1");
        ViNode viHost2 = cloud.node("node-2");

        ViNode group = Cloud.multiNode(viHost1, viHost2);

        viHost1.calcCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                StaticVarHost.TEST_STATIC_VAR = "isolate 1";
                return null;
            }
        });

        viHost2.calcCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                StaticVarHost.TEST_STATIC_VAR = "isolate 2";
                return null;
            }
        });

        Collection<String> results = group.massCalcCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return StaticVarHost.TEST_STATIC_VAR;
            }
        }).all();

        Assertions.assertThat(results).contains("isolate 1", "isolate 2");
    }

    @Test
    public void verify_isolated_static_with_runnable() {

        ViNode viHost1 = cloud.node("node-1");
        ViNode viHost2 = cloud.node("node-2");

        ViNode group = Cloud.multiNode(viHost1, viHost2);

        viHost1.execRunnable(new Runnable() {
            @Override
            public void run() {
                StaticVarHost.TEST_STATIC_VAR = "isolate 1";
            }
        });

        viHost2.execRunnable(new Runnable() {
            @Override
            public void run() {
                StaticVarHost.TEST_STATIC_VAR = "isolate 2";
            }
        });

        Collection<String> results = group.massCalcCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return StaticVarHost.TEST_STATIC_VAR;
            }
        }).all();

        Assertions.assertThat(results).contains("isolate 1", "isolate 2");
    }

    @Test
    public void verify_class_exclusion() {

        ViNode viHost1 = cloud.node("node-1");
        ViNode viHost2 = cloud.node("node-2");

        ViNode group = Cloud.multiNode(viHost1, viHost2);

        group.x(ISOLATE).shareClass(StaticVarHost.class);

        viHost1.execRunnable(new Runnable() {
            @Override
            public void run() {
                StaticVarHost.TEST_STATIC_VAR = "isolate 1";
            }
        });

        viHost2.execRunnable(new Runnable() {
            @Override
            public void run() {
                StaticVarHost.TEST_STATIC_VAR = "isolate 2";
            }
        });

        Collection<String> results = group.massCalcCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return StaticVarHost.TEST_STATIC_VAR;
            }
        }).all();

        Assertions.assertThat(results).contains("isolate 2", "isolate 2");

    }

    @Test
    public void verify_property_isolation() throws Exception {

        ViNode node1 = cloud.node("node-1");
        ViNode node2 = cloud.node("node-2");

        node1.execRunnable(new Runnable() {
            @Override
            public void run() {
                System.setProperty("local-prop", "Isolate1");
            }
        });

        node2.execRunnable(new Runnable() {
            @Override
            public void run() {
                System.setProperty("local-prop", "Isolate2");
            }
        });

        node1.execRunnable(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("Isolate1", System.getProperty("local-prop"));
            }
        });

        node2.execRunnable(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("Isolate2", System.getProperty("local-prop"));
            }
        });

        final String xxx = new String("Hallo from Isolate2");
        node2.execRunnable(new Runnable() {
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
            node.execRunnable(new Runnable() {
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

    @Test
    public void verify_isolate_native_proxy_stack_trace() {

        Isolate is1 = new Isolate("node-1", "com.tangosol", "org.gridkit");
        is1.start();

        try {
            Runnable r = is1.export(new Callable<Runnable>() {
                @Override
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
            assertLocalStackTrace(e);
        }
    }

    @Test
    public void verify_transparent_proxy_stack_trace() {

        ViNode node = cloud.node("node-1");

        try {
            Runnable r = node.calcCallable(new Callable<Runnable>() {
                @Override
                public Runnable call() {
                    return  new RemoteRunnable() {

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
    public void test_classpath_extention() throws IOException, URISyntaxException {

        ViNode node = cloud.node("test-node");

        URL jar = getClass().getResource("/marker-override.jar");
        File jarFile = new File(jar.toURI());
        node.x(VX.CLASSPATH).add(jarFile.getPath());

        node.calcCallable(new Callable<Void>() {

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

    @Test
    public void test_annonimous_primitive_in_args() {

        ViNode node = cloud.node("test_annonimous_primitive_in_args");

        final boolean fb = trueConst();
        final int fi = int_10();
        final double fd = double_10_1();

        node.calcCallable(new Callable<Void>() {

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

    public interface RemoteRunnable extends Runnable, Remote {

    }
}
