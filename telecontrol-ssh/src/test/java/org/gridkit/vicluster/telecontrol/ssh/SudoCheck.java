package org.gridkit.vicluster.telecontrol.ssh;

import static org.gridkit.nanocloud.RemoteNode.REMOTE;
import static org.gridkit.nanocloud.VX.JVM;

import java.util.concurrent.Callable;

import org.gridkit.nanocloud.SimpleCloudFactory;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class SudoCheck {

    private ViNodeSet cloud;

    @Before
    public void initCloud() {
        cloud = SimpleCloudFactory.createSimpleSshCloud();
    }

    @After
    public void dropCloud() {
        cloud.shutdown();
    }

    @Test
    public void bootstrap_sudo_check() {
        ViNode node = cloud.node("cbox1");

        node.x(REMOTE).setRemoteBootstrapJavaExec("sudo java");

        node.exec(new Callable<Void>(){

            @Override
            public Void call() throws Exception {
                System.out.println("ping");
                return null;
            }
        });
    }

    @Test
    public void slave_sudo_check() {
        ViNode node = cloud.node("cbox1");

        node.x(REMOTE).setRemoteBootstrapJavaExec("java");
        node.x(JVM).setJavaExec("sudo", "java");

        node.exec(new Callable<Void>(){

            @Override
            public Void call() throws Exception {
                System.out.println("ping");
                return null;
            }
        });
    }
}
