package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.VX.CONSOLE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViGroup;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("I've fogotten that is this all about")
public class ResilentCloudTest {

    String basePath = "target/resilence_test_" + System.currentTimeMillis();

    @Before
    public void initTestLayout() {
        File base  = new File(basePath);
        base.mkdirs();
        if (base.listFiles() != null) {
            for(File f: base.listFiles()) {
                f.delete();
                Assert.assertFalse(f.exists());
            }
        }
    }

    public Cloud setupCloud() {

        Cloud cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();

        cloud.node("node1").x(VX.PROCESS).setWorkDir(basePath + "/node1");
        cloud.node("node2").x(VX.PROCESS).setWorkDir(basePath + "/node2");
        cloud.node("node3").x(VX.PROCESS).setWorkDir(basePath + "/node3");

        new File(basePath + "/node1").mkdirs();
        new File(basePath + "/node3").mkdirs();

        return cloud;
    }

    @Test
    public void verify_startup_failure() {
        try {
            setupCloud().node("**").touch();
            Assert.fail("Should fail to start");
        }
        catch(Exception e) {
            // expected
        }
    }

    @Test
    public void verify_warmup_failure() {
        Cloud cloud = setupCloud();
        Assert.assertFalse(warmUp(cloud, "**", 0, TimeUnit.SECONDS));
    }

    @Test
    public void verify_recovery_from_failre() {
        Cloud cloud = setupCloud();
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    // fix node2
                    new File("target/resilence_test/node2").mkdirs();
                } catch (InterruptedException e) {
                    // ignore
                }
            };
        }.start();
        Assert.assertTrue(warmUp(cloud, "**", 5, TimeUnit.SECONDS));

        List<String> ping = cloud.node("**").massExec(new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.err.println(new File(".").getAbsolutePath());
                return "1";
            }
        });

        Assert.assertEquals(3, ping.size());

        cloud.node("**").x(CONSOLE).flush();
    }

    @Test
    public void verify_good_nodes_startup() {

        setupCloud().node("node1").touch();
        setupCloud().node("node3").touch();

    }

    public boolean warmUp(Cloud cloud, String selector, long recoverTimeout, TimeUnit tu) {
        try {
            cloud.node(selector).touch();
            return true;
        }
        catch(Exception e) {
            // ignore
        }
        List<ViNode> nodes = new ArrayList<ViNode>(cloud.listNodes(selector));

        long deadline = System.nanoTime() + tu.toNanos(recoverTimeout);
        recover(nodes);
        while(!nodes.isEmpty() && deadline > System.nanoTime()) {
            List<String> names = new ArrayList<String>();
            if (!nodes.isEmpty()) {
                for(ViNode node: nodes) {
                    names.add(node.getProp(ViConf.NODE_NAME));
                    node.kill();
                }
            }
            ((ViManager)cloud).resetDeadNode();
            nodes.clear();
            for(String name: names) {
                nodes.add(cloud.node(name));
            }
            recover(nodes);
        }

        try {
            cloud.node(selector).touch();
            return true;
        }
        catch(Exception e) {
            // ignore
        }
        return false;
    }

    private void recover(List<ViNode> suspects) {
        List<ViNode> batch = new ArrayList<ViNode>();
        for(ViNode n: suspects) {
            batch.add(n);
            if (batch.size() == 6) {
                recoverBatch(batch);
                suspects.removeAll(batch);
                batch.clear();
            }
        }
        recoverBatch(batch);
        suspects.removeAll(batch);
    }

    private void recoverBatch(List<ViNode> batch) {
        ViGroup vg = new ViGroup();
        for(ViNode vn: batch) {
            vg.addNode(vn);
        }
        try {
            vg.touch();
            // ok
            return;
        }
        catch(Exception e) {
            // ignore
        }
        List<ViNode> badNodes = new ArrayList<ViNode>();
        // touch every node
        for(ViNode vn: batch) {
            try {
                vn.touch();
            }
            catch (Exception e) {
                // bad node
                badNodes.add(vn);
            }
        }
        batch.removeAll(badNodes);
    }
}
