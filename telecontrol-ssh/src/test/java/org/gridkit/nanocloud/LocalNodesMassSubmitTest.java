package org.gridkit.nanocloud;

import org.junit.After;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.Future;

public class LocalNodesMassSubmitTest {

    private Cloud cloud;

    @After
    public void recycleCloud() {
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    @Test
    public void testMassSubmit() throws Exception {
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        cloud.nodes("node1", "node2", "node3");
        cloud.node("**").touch();

        List<Future<Void>> futures = cloud.node("**").massSubmit(new Runnable() {
            @Override
            public void run() {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                System.exit(1);
            }
        });

        for(Future future: futures){
            future.get();
        }
    }
}
