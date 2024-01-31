package org.gridkit.nanocloud;

import org.gridkit.nanocloud.telecontrol.ssh.SshConf;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class SshAuthenticationTest {

    @BeforeClass
    public static void check_cbox1() {
        Cloud c = Nanocloud.createCloud();
        try {
            c.x(SshConf.SSH)
                .setRemoteHost("cbox1")
                .setSshConfigFile("?~/ssh-credentials.prop");

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

    @Test
    @SuppressWarnings("deprecation")
    public void verify_password_auth() {

        Cloud c = Nanocloud.createCloud();
        c.x(SshConf.SSH)
            .setRemoteHost("cbox1")
            .setRemoteAccount("root")
            .setPassword("reverse");

        // cbox1 is hostname of VM for integration testing
        c.node("cbox1").execRunnable(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello nanocloud");
            }
        });

    }

    @Test
    @SuppressWarnings("deprecation")
    public void verify_password_auth_with_simple_preset() {

        Cloud c = Nanocloud.createCloud();
        c.x(SshConf.SSH)
            .configureSimpleRemoting()
            .setSshConfigFile("?na")
            .setRemoteAccount("root")
            .setPassword("reverse");


        // cbox1 is hostname of VM for integration testing
        c.node("cbox1").execRunnable(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello nanocloud");
            }
        });

    }

    @Test
    public void verify_key_auth() {

        String pk = (System.getProperty("user.home") + "/id_dsa.cbox1").replace('\\', '/');

        Cloud c = Nanocloud.createCloud();
        c.x(SshConf.SSH);
        c.node("**").setProp(RemoteNode.HOST, "cbox1");

        c.node("**").x(RemoteNode.REMOTE).setRemoteAccount("root");
        c.node("**").x(RemoteNode.REMOTE).setSshPrivateKey(pk);

        // cbox1 is hostname of VM for integration testing
        c.node("cbox1").execRunnable(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello nanocloud");
            }
        });
    }

    @Test
    public void verify_key_auth_with_simple_preset() {

        String pk = (System.getProperty("user.home") + "/id_dsa.cbox1").replace('\\', '/');

        Cloud c = Nanocloud.createCloud();
        c.x(SshConf.SSH)
            .configureSimpleRemoting();

        c.node("**").x(RemoteNode.REMOTE).setHostsConfigFile("?na"); // turn off host config file
        c.node("**").x(RemoteNode.REMOTE).setRemoteAccount("root");
        c.node("**").x(RemoteNode.REMOTE).setSshPrivateKey(pk);

        // cbox1 is hostname of VM for integration testing
        c.node("cbox1").execRunnable(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello nanocloud");
            }
        });
    }
}
