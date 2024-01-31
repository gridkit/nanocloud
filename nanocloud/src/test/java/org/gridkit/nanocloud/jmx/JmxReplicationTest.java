package org.gridkit.nanocloud.jmx;

import static org.gridkit.nanocloud.VX.ISOLATE;
import static org.gridkit.nanocloud.VX.TYPE;

import java.lang.management.ManagementFactory;
import java.lang.reflect.UndeclaredThrowableException;

import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.gridkit.nanocloud.VX;
import org.gridkit.nanocloud.ViNode;
import org.gridkit.nanocloud.interceptor.Intercept;
import org.gridkit.nanocloud.interceptor.misc.PlatformMBeanServerInterceptor;
import org.gridkit.nanocloud.jmx.MBeanRegistrator.DestroyableMBeanRegistrator;
import org.gridkit.nanocloud.test.junit.CloudRule;
import org.gridkit.nanocloud.test.junit.DisposableCloud;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class JmxReplicationTest {

    public static ObjectName mname(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String TEST_BEAN_NAME ="NanocloudTest:name=TEST_BEAN";

    @Rule
    public CloudRule cloud = new DisposableCloud();

    @Test
    public void replicate_remote_process_mbeans() throws InterruptedException {

        ViNode node = cloud.node("target");
        node.x(VX.LOCAL);

        DestroyableMBeanRegistrator mreg = new DestroyableMBeanRegistrator(new MBeanRegistrator.MBeanDomainPrefixer(new MBeanRegistrator.MBeanServerRegistrator(ManagementFactory.getPlatformMBeanServer()), "target@"));

        final RemoteMBeanReplicator mrep = new RemoteMBeanReplicator(mreg);

        node.exec(() -> {
            mrep.export(null, ManagementFactory.getPlatformMBeanServer());
            addTestMBean("JMX_TEST");
        });

        Thread.sleep(1000);

        TestDataMBean proxy = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), mname("target@" + TEST_BEAN_NAME), TestDataMBean.class);
        Assert.assertEquals("JMX_TEST", proxy.getName());

        mreg.destroy();

        try {
            JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), mname("target@" + TEST_BEAN_NAME), TestDataMBean.class).getName();
            Assert.fail("Exception expected");
        }
        catch(UndeclaredThrowableException e) {
            Assert.assertEquals(InstanceNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test
    public void replicate_isolate_mbeans() throws InterruptedException {

//		System.setProperty("gridkit.interceptor.trace", "true");

        ViNode node1 = cloud.node("isolate1");
        ViNode node2 = cloud.node("isolate2");

        node1.x(TYPE).setIsolate();
        node2.x(TYPE).setIsolate();

        // Since Java 11 javax packages must be shared in most cases
        node1.x(ISOLATE).sharePackage("javax.management");
        node2.x(ISOLATE).sharePackage("javax.management");

        PlatformMBeanServerInterceptor.apply(node1);
        PlatformMBeanServerInterceptor.apply(node2);

        Intercept.enableInstrumentationTracing(node1, true);

        DestroyableMBeanRegistrator mreg1 = new DestroyableMBeanRegistrator(new MBeanRegistrator.MBeanDomainPrefixer(new MBeanRegistrator.MBeanServerRegistrator(ManagementFactory.getPlatformMBeanServer()), "isolate1@"));
        final RemoteMBeanReplicator mrep1 = new RemoteMBeanReplicator(mreg1);

        node1.exec(() -> {
            MBeanServer mserver = ManagementFactory.getPlatformMBeanServer();
            System.out.println("MServer is " + mserver.getClass().getName());
            mrep1.export(null, mserver);
        });

        DestroyableMBeanRegistrator mreg2 = new DestroyableMBeanRegistrator(new MBeanRegistrator.MBeanDomainPrefixer(new MBeanRegistrator.MBeanServerRegistrator(ManagementFactory.getPlatformMBeanServer()), "isolate2@"));
        final RemoteMBeanReplicator mrep2 = new RemoteMBeanReplicator(mreg2);

        node2.exec(() -> {
            MBeanServer mserver = ManagementFactory.getPlatformMBeanServer();
            System.out.println("MServer is " + mserver.getClass().getName());
            mrep2.export(null, mserver);
        });

        node1.exec(() -> {
            addTestMBean("Isolate1");
        });
        node2.exec(() -> {
            addTestMBean("Isolate2");
        });

        Thread.sleep(1000);

        TestDataMBean proxy1 = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), mname("isolate1@" + TEST_BEAN_NAME), TestDataMBean.class);
        Assert.assertEquals("Isolate1", proxy1.getName());

        TestDataMBean proxy2 = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), mname("isolate2@" + TEST_BEAN_NAME), TestDataMBean.class);
        Assert.assertEquals("Isolate2", proxy2.getName());

        mreg1.destroy();

        // Verify that second is still accessible
        Assert.assertEquals("Isolate2", proxy2.getName());

        try {
            JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), mname("isolate1@" + TEST_BEAN_NAME), TestDataMBean.class).getName();
            Assert.fail("Exception expected");
        }
        catch(UndeclaredThrowableException e) {
            Assert.assertEquals(InstanceNotFoundException.class, e.getCause().getClass());
        }

        mreg2.destroy();

        try {
            JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), mname("isolate2@" + TEST_BEAN_NAME), TestDataMBean.class).getName();
            Assert.fail("Exception expected");
        }
        catch(UndeclaredThrowableException e) {
            Assert.assertEquals(InstanceNotFoundException.class, e.getCause().getClass());
        }
    }


    private static void addTestMBean(String value) {
        try {
            MBeanServer mserver = ManagementFactory.getPlatformMBeanServer();
            System.out.println("MServer is " + mserver.getClass().getName());
            mserver.registerMBean(new TestData(value), mname(TEST_BEAN_NAME));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
