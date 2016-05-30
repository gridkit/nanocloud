package org.gridkit.nanocloud.tunneller;

import static org.gridkit.nanocloud.RemoteNode.REMOTE;
import static org.gridkit.nanocloud.VX.CLASSPATH;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.vicluster.ViNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class TunnelerRecoveryTest {

    private static String account;
    private static String password;
    
    @BeforeClass
    public static void check_cbox1() throws IOException {
        Cloud c = CloudFactory.createCloud();
        try {
            c.node("**").x(REMOTE)
                .useSimpleRemoting()
                .setRemoteHost("cbox1");
            
            c.node("test").touch();
            account = c.node("test").getProp("#spi:ssh:target-account");
            password = c.node("test").getProp("#spi:ssh:password");
            c.shutdown();
        }
        catch(Exception e) {
            e.printStackTrace();
            Assume.assumeTrue(false);
        }
        finally {
            c.shutdown();
        }
        
        initHost();
    }
    
    public static SimpleProxy proxy;
    public static Cloud cloud = CloudFactory.createCloud();

    public static void initHost() throws IOException { 
        proxy = new SimpleProxy(InetSocketAddress.createUnresolved("cbox1", 22));
        ViNode host = cloud.node("**");
        host.x(REMOTE).useSimpleRemoting();
        host.x(REMOTE).setRemoteHost(proxy.getLocalAddress().getHostName() + ":" + proxy.getLocalAddress().getPort());
        host.x(REMOTE).setRemoteAccount(account);
        host.x(REMOTE).setPassword(password);
        host.x(CLASSPATH).add("../vicluster-core/target/test-classes");
    }

    @AfterClass
    public static void dropCloud() {
        cloud.shutdown();
        proxy.shutdown();
    }
    
    @Test
    public void ping() {
        cloud.node("ping").exec(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("Ping");                
            }
        });
    }
    
    @Test
    public void testRecovery() {
        ViNode node1 = cloud.node("node1"); 
        node1.exec(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("Ping");                
            }
        });
        
        proxy.dropConnections();
        
        try {
            node1.exec(new Runnable() {
                
                @Override
                public void run() {
                    System.out.println("Ping");                
                }
            });
            Assert.fail("Exception expected");
        }
        catch(Exception e) {
            Assert.assertEquals(RemoteException.class.getName(), e.getClass().getName());
        }

        ViNode node2 = cloud.node("node2"); 
        try {
            node2.touch();
            Assert.fail("Exception expected");
        }
        catch(Exception e) {
            // expected
        }
        
        ViNode node3 = cloud.node("node3"); 
        node3.exec(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("Ping");                
            }
        });
    }    
}
