package org.gridkit.vicluster.telecontrol;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class CleanShutdownTest {

    @Rule
    public TestName testName = new TestName();
    
    @Test
    public void verify_local_node_clean_shutdown() throws InterruptedException, ExecutionException {
        
        ThreadGroupRunner tgr = new ThreadGroupRunner(testName.getMethodName());
        
        FutureTask<Void> ft = new FutureTask<Void>(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                
                Cloud cloud = CloudFactory.createCloud();
                cloud.node("**").x(VX.TYPE).setLocal();
                
                cloud.node("A");
                cloud.node("B");
                cloud.node("C");
                cloud.node("D");
                
                cloud.node("**").exec(new Runnable() {
                    
                    @Override
                    public void run() {
                        System.out.println("Hallo cloud");                        
                    }
                });
                
                cloud.shutdown();
                
                return null;
            }
        });
        
        tgr.submit(ft);
        
        ft.get();
        
        // TODO implement wait helper
        Thread.sleep(1000);
        
        tgr.assertNoThreads();
    }
    
    static class ThreadGroupRunner {
        
        ThreadGroup tg;
        
        public ThreadGroupRunner(String name) {
            tg = new ThreadGroup(name);
        }
                
        public void submit(Runnable runnable) {
            Thread t = new Thread(tg, runnable);
            t.start();
        }
        
        public void assertNoThreads() {
            Thread[] t = new Thread[10];
            int n = tg.enumerate(t, true);
            for(int i = 0; i != n; ++i) {
                System.out.println("Left thread:" + t[i].getName());
            }
            if (n > 0) {
                Assert.fail("No have no thread left, " + n + " remaining");
            }
        }
        
        public int getThreadCount() {
            return tg.activeCount();
        }
    }    
}
