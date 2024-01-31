package org.gridkit.nanocloud;

import org.gridkit.vicluster.ViNodeSet;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import junit.framework.Assert;

import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressWarnings("deprecation")
public class NodeTerminationExceptionTest {

    @Rule
    public Timeout timeout = new Timeout(30000);

    private ViNodeSet cloud;

    @After
    public void recycleCloud() {
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    @Test(expected = RemoteException.class)
    public void submit_verify_execption_on_node_termination() throws Exception {
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        cloud.nodes("node1");
        cloud.node("**").touch();

        Future<Void> future = cloud.node("**").submit(new Runnable() {
            @Override
            public void run() {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                System.exit(1);
            }
        });

        future.get();
    }

    @Test(expected = RemoteException.class)
    public void exec_verify_execption_on_node_termination() throws Exception {
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        cloud.nodes("node1");
        cloud.node("**").touch();

        cloud.node("**").exec(new Runnable() {
            @Override
            public void run() {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello!");
                System.exit(1);
            }
        });
    }

    @Test
    public void massSubmit_verify_execption_on_node_termination() throws InterruptedException {
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

        for(Future<Void> future: futures){
            try {
                future.get();
            }
            catch(ExecutionException e) {
                Assert.assertEquals(RemoteException.class.getName(), e.getCause().getClass().getName());
            }
        }
    }
}
