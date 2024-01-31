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

import static org.gridkit.nanocloud.VX.CONSOLE;

import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.Nanocloud;
import org.gridkit.nanocloud.VX;
import org.gridkit.nanocloud.ViNode;
import org.gridkit.nanocloud.interceptor.Intercept;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InstrumentationFeatureTest {

    protected Cloud cloud;

    @Before
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(VX.TYPE).setIsolate();
    }

    @After
    public void dropCloud() {
        cloud.shutdown();
    }

    protected ViNode node(String name) {
        return cloud.node(name);
    }

    @Test
    public void test_print_rule() {

        StringWriter writer = new StringWriter();
        ViNode node = node("test_print_rule");

        node.x(CONSOLE)
            .bindOut(writer)
            .echoOut(true);

        Intercept.callSite()
            .onTypes(System.class)
            .onMethod("currentTimeMillis")
            .doPrint("Call time")
            .apply(node);

        node.calc(() -> {
            return System.currentTimeMillis();
        });

        node.x(CONSOLE).flush();

        Assert.assertTrue("\"Call time\" text is expected", writer.toString().contains("Call time"));
    }

    @Test
    public void test_instrumentation_return_value() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");

        ViNode node = node("test_instrumentation_return_value");

        Intercept.callSite()
            .onTypes(System.class)
            .onMethod("currentTimeMillis")
            .doInvoke(new LongReturnValueShifter(-111111))
            .apply(node);

        long time = System.currentTimeMillis();

        long itime = node.calc(() -> {
            return System.currentTimeMillis();
        });

        Assert.assertTrue("Time expected to be shifted back", itime < time);
    }

    @Test
    public void test_instrumentation_expection_fallthrough() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");

        ViNode node = node("test_instrumentation_expection_fallthrough");

        Intercept.callSite()
            .onTypes(InstrumentationFeatureTest.class)
            .onMethod("explode")
            .doInvoke(new LongReturnValueShifter(-111111))
            .apply(node);

        node.calc(() -> {
            try {
                explode("test");
                Assert.fail("Exception expected");
            }
            catch(IllegalStateException e) {
                Assert.assertEquals("test", e.getMessage());
            }
            return null;
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

        Intercept
        .callSite()
        .onTypes(System.class)
        .onMethod("exit")
        .doReturn(null)
        .apply(node);

        node.exec(() -> {
            System.exit(0);
        });
    }

    @Test
    public void test_instrumentation_execution_prevention2() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");

        ViNode node = node("test_instrumentation_execution_prevention");

        Intercept
        .callSite()
        .onTypes(System.class)
        .onMethod("exit")
        .doReturn(null)
        .apply(node);

        node.exec(() -> {
            System.exit(0);
            // May be second time?
            System.exit(0);
        });
    }

    @Test(expected=IllegalStateException.class)
    public void test_instrumentation_exception() {
//		System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");

        ViNode node = node("test_instrumentation_exception");

        Intercept
        .callSite()
        .onTypes(System.class)
        .onMethod("exit")
        .doThrow(new IllegalStateException("Ka-Boom"))
        .apply(node);

        node.exec(() -> {
            System.exit(0);
        });
    }

    private void addValueRule(ViNode node, Object key, Object value) {
        Intercept
        .callSite()
        .onTypes(InstrumentationFeatureTest.class)
        .onMethod("getSomething")
        .matchParams(key)
        .doReturn(value)
        .apply(node);
    }

    private void addErrorRule(ViNode node, Object key, Throwable e) {
        Intercept
        .callSite()
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

        ViNode node = node("test_instrumentation_handler_staking");

        addValueRule(node, "A", "a");
        addValueRule(node, "B", "b");
        addValueRule(node, "B", "bb");
        addErrorRule(node, "X", new IllegalStateException("Just for fun"));

        node.exec(() -> {

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
        });
    }

    private static Object getSomething(Object key) {
        return null;
    }

    @Test
    public void test_instrumentation_call_counter() {
        System.setProperty("gridkit.isolate.trace-classes", "true");
//		System.setProperty("gridkit.interceptor.trace", "true");

        ViNode node = node("test_instrumentation_handler_staking");

        AtomicLong callA = new AtomicLong();
        AtomicLong callB = new AtomicLong();

        Intercept.callSite()
            .onTypes(InstrumentationFeatureTest.class)
            .onMethod("callA", new Class<?>[0])
            .doCount(callA)
            .apply(node);

        Intercept.callSite()
            .onTypes(InstrumentationFeatureTest.class)
            .onMethod("callB", new Class<?>[0])
            .doCount(callB)
            .apply(node);

        node.exec(() -> {
            callA();
            callB();
            callA();
        });

        Assert.assertEquals(2, callA.get());
        Assert.assertEquals(1, callB.get());
    }

    private static void callA() {};

    private static void callB() {};


    @SuppressWarnings("serial")
    public static class LongReturnValueShifter implements Interceptor {

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
