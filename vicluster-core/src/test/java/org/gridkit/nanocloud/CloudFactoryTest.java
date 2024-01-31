package org.gridkit.nanocloud;

import java.io.StringWriter;
import java.util.concurrent.Callable;

import org.gridkit.sjk.test.console.junit4.ConsoleRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import junit.framework.Assert;

public class CloudFactoryTest {

    @Rule
    public ConsoleRule console = ConsoleRule.out();

    public Cloud cloud = initCloud();

    public Cloud initCloud() {
        Cloud cloud = Nanocloud.createCloud();
        cloud.x(VX.LOCAL);
        return cloud;
    }

    @After
    public void dropCloud() {
        cloud.shutdown();
    }

    @Test
    public void ping_local_node() {
        ViNode node = cloud.node("test");
        node.touch();
        String r = node.calcCallable(new Callable<String>() {
            @Override
            public String call() {
                System.out.println("Hallo world!");
                return"ping";
            }
        });

        Assert.assertEquals("ping", r);
    }

    @Test
    public void capture_console_local_node() throws InterruptedException {
        ViNode node = cloud.node("test");
        node.touch();

        StringWriter outwriter = new StringWriter();
        StringWriter errwriter = new StringWriter();
        node.x(VX.CONSOLE).bindOut(outwriter);
        node.x(VX.CONSOLE).bindOut(outwriter);
        node.x(VX.CONSOLE).bindErr(errwriter);

        String r = node.calcCallable(new Callable<String>() {
            @Override
            public String call() {
                System.out.println("ping");
                System.err.println("pong");
                return"ping";
            }
        });

        node.x(VX.CONSOLE).flush();

        Assert.assertEquals("ping", r);
        Thread.sleep(200);
        Assert.assertTrue(outwriter.toString().startsWith("ping"));
        Assert.assertTrue(errwriter.toString().startsWith("pong"));

        node.x(VX.CONSOLE).echoPrefix("~[%s-xx] !(.*)");

        node.calcCallable(new Callable<Void>() {
            @Override
            public Void call() {
                System.out.println("This line should use diffent prefix");
                return null;
            }
        });

        node.x(VX.CONSOLE).flush();

        console.skip();
        console.line("[test] ping");
        console.line("[test-xx] This line should use diffent prefix");
    }
}
