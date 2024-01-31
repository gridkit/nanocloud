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
package org.gridkit.vicluster.isolate;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.Nanocloud;
import org.gridkit.nanocloud.VX;
import org.gridkit.nanocloud.ViNode;
import org.gridkit.nanocloud.interceptor.Intercept;
import org.gridkit.util.concurrent.CyclicBlockingBarrier;
import org.gridkit.util.concurrent.VectorFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class InstrumentationSymphonizingTest {

    @ClassRule
    public static Timeout TIMEOUT = new Timeout(600000);

    private Cloud cloud;

    @Before
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(VX.ISOLATE);
    }

    @After
    public void shutdownCloud() {
        cloud.shutdown();
    }

    private static StringBuffer text = new StringBuffer();

    private static void pushToList(String value) {
        text.append(value);
    }

    @Test
    public void verify_call_ordering_is_broken_without_barrier() throws InterruptedException, ExecutionException {

        ViNode node = cloud.node("test-node");

        Future<?> f = sendTasks(node);

        f.get();

        String text = node.calc(() -> {
            return InstrumentationSymphonizingTest.text.toString();
        });

        Assert.assertFalse("ABCDEFGHI".equals(text));
    }

    @Test
    public void verify_call_ordering_with_barriers() throws InterruptedException, ExecutionException, BrokenBarrierException {

        ViNode node = cloud.node("test-node");

        CyclicBlockingBarrier barA = new CyclicBlockingBarrier(2, null);
        CyclicBlockingBarrier barB = new CyclicBlockingBarrier(2, null);
        CyclicBlockingBarrier barC = new CyclicBlockingBarrier(2, null);
        CyclicBlockingBarrier barD = new CyclicBlockingBarrier(2, null);
        CyclicBlockingBarrier barE = new CyclicBlockingBarrier(2, null);
        CyclicBlockingBarrier barF = new CyclicBlockingBarrier(2, null);
        CyclicBlockingBarrier barG = new CyclicBlockingBarrier(2, null);
        CyclicBlockingBarrier barH = new CyclicBlockingBarrier(2, null);

        injectBarrier(node, "A", barA);
        injectBarrier(node, "B", barB);
        injectBarrier(node, "C", barC);
        injectBarrier(node, "D", barD);
        injectBarrier(node, "E", barE);
        injectBarrier(node, "F", barF);
        injectBarrier(node, "G", barG);
        injectBarrier(node, "H", barH);

        Future<?> f = sendTasks(node);

        barA.pass();
        barB.pass();
        barC.pass();
        barD.pass();
        barE.pass();
        barF.pass();
        barG.pass();
        barH.pass();

        f.get();

        String text = node.calc(() -> {
            return InstrumentationSymphonizingTest.text.toString();
        });

        Assert.assertFalse("ABCDEFGHI".equals(text));
    }

    private void injectBarrier(ViNode node, String value, CyclicBlockingBarrier barA) {
        Intercept.callSite()
            .onTypes(InstrumentationSymphonizingTest.class)
            .onMethod("pushToList", String.class)
            .matchParam(0, value)
            .doBarrier(barA)
            .apply(node);
    }

    @SuppressWarnings("unchecked")
    private Future<List<Void>> sendTasks(ViNode node) {
        Future<Void> f1 = node.asyncExecRunnable(new Runnable() {
            @Override
            public void run() {
                pushToList("A");
                pushToList("D");
                pushToList("G");
            }
        });
        Future<Void> f2 = node.asyncExecRunnable(new Runnable() {
            @Override
            public void run() {
                pushToList("B");
                pushToList("E");
                pushToList("H");
            }
        });
        Future<Void> f3 = node.asyncExecRunnable(new Runnable() {
            @Override
            public void run() {
                pushToList("C");
                pushToList("F");
                pushToList("I");
            }
        });
        return VectorFuture.lump(f1, f2, f3);
    }
}
