package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.SimpleCloudFactory;
import org.gridkit.nanocloud.VX;
import org.junit.Test;

public class SocketCloseCheck {

    @Test
    public void test_is_available() throws UnknownHostException, IOException {
        System.out.println("cbox1 is reachable " + Inet4Address.getByName("cbox1").isReachable(5000));
    }

    @Test
    public void cbox1_ping() throws UnknownHostException, IOException {
        Cloud cloud = SimpleCloudFactory.createSimpleSshCloud();
        cloud.node("cbox1").exec(new Runnable() {
            @Override
            public void run() {
            }
        });
    }
    
    @Test
    public void verify_cbox_cluster() throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
        Cloud cloud = SimpleCloudFactory.createSimpleSshCloud();
//        Cloud cloud = CloudFactory.createCloud();
//        cloud.node("cbox1").x(VX.TYPE).setLocal();
//        final String targetHost = "127.0.0.1"; 
        final String targetHost = "cbox1"; 
        
        cloud.node("cbox1");
        
        final CountDownLatch latch = new CountDownLatch(1);
        final RemoteLatch rlatch = new RemoteLatch() {
            @Override
            public void open() {
                latch.countDown();                
            }
            
            @Override
            public void await() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                }
            }
        };

        final int port = 33033;
        Future<Void> server = cloud.node("cbox1").submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException, InterruptedException {
                ServerSocket sock = new ServerSocket();
                System.out.println("Starting server");
                sock.bind(new InetSocketAddress(targetHost, port));
                rlatch.open();
                Thread.sleep(10000);
                System.out.println(sock.getLocalSocketAddress());
                Socket soc = sock.accept();
//                soc.setSoTimeout(1000);
                System.out.println("Socket accepted");
                int byteCount = 0;
//                Thread.sleep(1000);
                byte[] buf = new byte[64 << 10];
                InputStream is = soc.getInputStream();
                while(true) {
                    int n;
                    try {
                        n = is.read(buf, 0, buf.length);
                    }
                    catch(IOException e) {
                        System.out.println("Inbound stream error: " + e.toString());
                        break;
                    }
                    if (n == -1) {
                        System.out.println("Inbound stream closed");
                        soc.close();
                        break;
                    }
                    byteCount += n;
                }
                System.out.println("Total read: " + byteCount);
                return null;                
            }
        });
        
        latch.await();
        Thread.sleep(100);
        System.out.println("Connecting to socket");
        Socket sock = new Socket(targetHost, port);
        System.out.println("Connected");
        sock.setSoTimeout(1000);
//        sock.setSoLinger(true, 0);
//        sock.setSendBufferSize(256 << 10);;
        byte[] b = new byte[1 << 20];
        for(int i = 0; i != 10000; ++i) {
            sock.getOutputStream().write(b);
        }
//        Thread.sleep(10000);
        sock.getOutputStream().write(new byte[10000 << 10]);
        long t = System.nanoTime();
        sock.getOutputStream().close();
        System.out.println("Close time " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t));
        
        server.get(60, TimeUnit.SECONDS);
        System.out.println("flushing ...");
        cloud.node("cbox1").x(VX.CONSOLE).flush();
        System.out.println("... done");
    }

    @Test
    public void verify_read_timeout() throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
        Cloud cloud = SimpleCloudFactory.createSimpleSshCloud();
        final String targetHost = "cbox1"; 
        
        cloud.node(targetHost);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final RemoteLatch rlatch = new RemoteLatch() {
            @Override
            public void open() {
                latch.countDown();                
            }

            @Override
            public void await() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                }
            }
        };

        final int port = 33034;
        cloud.node(targetHost).submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException, InterruptedException {
                ServerSocket sock = new ServerSocket();
                System.out.println("Starting server");
                sock.bind(new InetSocketAddress(targetHost, port));
                System.out.println(sock.getLocalSocketAddress());
                Socket soc = sock.accept();
                System.out.println("Socket accepted");
                rlatch.await();
                System.out.println("Sending one byte");
                soc.getOutputStream().write(100);
                soc.shutdownOutput();
                soc.shutdownInput();
                return null;                
            }
        });
        
        Thread.sleep(5000);
        Socket sock = new Socket();
        InetSocketAddress endpoint = new InetSocketAddress("192.168.100.201", port);
        System.out.println("Connecting to socket: " + endpoint);
        sock.connect(endpoint);
        System.out.println("Connected");
        sock.setSoTimeout(10);
        try {
            sock.getInputStream().read();
        } catch (SocketTimeoutException e) {
            System.out.println("Got SocketTimeoutException");
        }
        sock.setSoTimeout(100000);
        latch.countDown();
        System.out.println("Read from socket: " + sock.getInputStream().read());
        
        System.out.println("flushing ...");
        cloud.node("cbox1").x(VX.CONSOLE).flush();
        System.out.println("... done");
    }
    
    public interface RemoteLatch extends Remote {
        
        public void open();
        
        public void await();
    }
}
