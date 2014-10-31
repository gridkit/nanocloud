package org.gridkit.vicluster.telecontrol;

import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(JMock.class)
public class StdOutputTest {
    Mockery mockery = new Mockery();
    private PrintStream oldOut;
    private PrintStream oldErr;
    private ViManager cloud;

    @Before
    public void setUp() throws Exception {
        mockery.setImposteriser(ClassImposteriser.INSTANCE);
        mockery.setThreadingPolicy(new Synchroniser());

        oldOut = System.out;
        oldErr = System.err;
        cloud = new ViManager(new JvmNodeProvider(new LocalJvmProcessFactory()));
        final ViNode node = cloud.node("start-node");
        node.touch();
    }

    @After
    public void tearDown() throws Exception {
        System.setOut(oldOut);
        System.setErr(oldErr);
        cloud.shutdown();
    }

    @Test
    public void testCorrectInheritedThreadLocal() throws Exception {
        final PrintStream mockStdOut = mockery.mock(PrintStream.class, "mockStdOut");
        final PrintStream mockStdErr = mockery.mock(PrintStream.class, "mockStdErr");

        System.setOut(mockStdOut);
        System.setErr(mockStdErr);

        final InheritableThreadLocal<OutputStream> outputStreamHolder = new InheritableThreadLocal<OutputStream>();
        final InheritableThreadLocal<OutputStream> errorOutputStreamHolder = new InheritableThreadLocal<OutputStream>();

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        PrintStream myStdOutStream = new PrintStream(outByteStream);

        ByteArrayOutputStream errByteStream = new ByteArrayOutputStream();
        PrintStream myStdErrStream = new PrintStream(errByteStream);

        mockery.checking(new Expectations() {{
            allowing(mockStdErr);
            will(delegateToThreadLocal(errorOutputStreamHolder));

            allowing(mockStdOut);
            will(delegateToThreadLocal(outputStreamHolder));
        }});


        outputStreamHolder.set(myStdOutStream);
        errorOutputStreamHolder.set(myStdErrStream);

        final ViNode node = cloud.node("test-node-node");
        node.exec(new Runnable() {
            @Override
            public void run() {
                System.out.println("I am System.out");
                System.err.println("I am System.err");
            }
        });
        
        Thread.sleep(500);
         
        assertTrue(outByteStream.toString().contains("I am System.out"));
        assertTrue(errByteStream.toString().contains("I am System.err"));
    }

    private Action delegateToThreadLocal(final InheritableThreadLocal<OutputStream> outputStreamHolder) {
        return new Action() {
            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                final OutputStream outputStream = outputStreamHolder.get();
                return invocation.applyTo(outputStream);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("delegate to " + outputStreamHolder);
            }
        };
    }
}
