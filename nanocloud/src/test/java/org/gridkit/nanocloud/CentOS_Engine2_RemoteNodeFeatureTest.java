package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.telecontrol.ssh.SshConf.SSH;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

public class CentOS_Engine2_RemoteNodeFeatureTest extends ViNodeFeatureTest {

    @BeforeClass
    public static void check_cbox1() {
        Cloud c = Nanocloud.createCloud();
        try {
            c.x(SSH)
                .configureSimpleRemoting()
                .setRemoteHost("cbox1");

            c.node("test").touch();
            c.shutdown();
        }
        catch(Exception e) {
            e.printStackTrace();
            Assume.assumeTrue(false);
        }
        finally {
            c.shutdown();
        }
    }

    @Before
    @Override
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(SSH)
            .configureSimpleRemoting()
            .setRemoteHost("cbox1");
    }
}
