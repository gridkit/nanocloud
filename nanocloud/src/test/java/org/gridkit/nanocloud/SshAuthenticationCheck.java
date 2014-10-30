package org.gridkit.nanocloud;

import org.junit.Test;

public class SshAuthenticationCheck {

    @Test
    @SuppressWarnings("deprecation")
    public void verify_password_auth() {
        
        Cloud c = CloudFactory.createCloud();
        c.node("**").x(RemoteNode.REMOTE).useSimpleRemoting();
        
        c.node("**").x(RemoteNode.REMOTE).setHostsConfigFile("?na"); // turn off host config file
        c.node("**").x(RemoteNode.REMOTE).setRemoteAccount("root");
        c.node("**").x(RemoteNode.REMOTE).setPassword("toor");
        
        // cbox1 is hostname of VM for integration testing
        c.node("cbox1").exec(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello nanocloud");
            }
        });
        
    }
}
