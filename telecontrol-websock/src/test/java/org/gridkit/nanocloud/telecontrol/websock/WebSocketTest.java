package org.gridkit.nanocloud.telecontrol.websock;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.nanocloud.telecontrol.websock.agent.WebSocketNanoAgent;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.FileHandler;
import org.gridkit.zerormi.DuplexStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class WebSocketTest {

    private WebSocketNanoAgent agent;

    @Before
    public void init() {
        agent = new WebSocketNanoAgent(null, 2345);
    }

    @After
    public void close() {
        agent.stop();
    }

    @Test
    public void handshakeTest() throws IOException, InterruptedException, TimeoutException, ExecutionException {

        WebSocketConnector connector = new WebSocketConnector("ws://127.0.0.1:2345");

        agent.start();

        TunnellerConnection conn = connect(connector.connect());

        TestFile tfile = new TestFile("test");
        conn.pushFile("{tmp}/.nanotestfile", tfile);

        tfile.done.get(10, TimeUnit.SECONDS);

        System.out.println("Remote path is " + tfile.remotePath);
    }

    @Test
    public void verifyFailureHandling() throws IOException, InterruptedException, TimeoutException, ExecutionException {

        WebSocketConnector connector = new WebSocketConnector("ws://127.0.0.1:2345");

        agent.start();

        TunnellerConnection conn = connect(connector.connect());

        BrokenTestFile tfile = new BrokenTestFile();
        conn.pushFile("{tmp}/." + System.currentTimeMillis() + ".test", tfile);

        try {
            tfile.done.get(10, TimeUnit.SECONDS);
            Assert.fail("Exception is expected");
        } catch (ExecutionException e) {
            // ok
        }
    }


    public TunnellerConnection connect(DuplexStream stream) throws IOException, InterruptedException, TimeoutException {

        TunnellerConnection conn = new TunnellerConnection("test", stream.getInput(), stream.getOutput(), System.out, 5, TimeUnit.SECONDS);
        return conn;
    }

    public static class BrokenTestFile implements FileHandler {

        FutureBox<Void> done = new FutureBox<Void>();
        String remotePath;
        long remoteSize;

        @Override
        public void accepted(OutputStream out) {
            try {
                out.write(0);
            } catch (IOException e) {
                // ignore
            }
            // leave stream hanged
        }

        @Override
        public void confirmed(String path, long size) {
            remotePath = path;
            remoteSize = size;
            done.setData(null);
        }

        @Override
        public void failed(String path, String error) {
            done.setError(new RuntimeException("path: " + path + " error: " + error));
        }
    }

    public static class TestFile implements FileHandler {

        private final String data;

        FutureBox<Void> done = new FutureBox<Void>();
        String remotePath;
        long remoteSize;

        protected TestFile(String data) throws IOException, SecurityException {
            this.data = data;
        }

        @Override
        public void accepted(OutputStream out) {
            try {
                out.write(data.getBytes());
                out.close();
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public void confirmed(String path, long size) {
            remotePath = path;
            remoteSize = size;
            done.setData(null);
        }

        @Override
        public void failed(String path, String error) {
            done.setError(new RuntimeException("path: " + path + " error: " + error));
        }
    }
}
