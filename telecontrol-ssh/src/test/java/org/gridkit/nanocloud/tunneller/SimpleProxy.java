package org.gridkit.nanocloud.tunneller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SimpleProxy {

    ServerSocket socket;
    InetSocketAddress targetAddress;
    InetSocketAddress localAddress;
    List<Socket> sockets = new ArrayList<Socket>();
    
    public SimpleProxy(InetSocketAddress targetAddress) throws IOException {
        this.targetAddress = targetAddress;
        socket = new ServerSocket(0);
        startAcceptor();
        localAddress = (InetSocketAddress) socket.getLocalSocketAddress();
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }
    
    public synchronized void dropConnections() {
        for(Socket s: sockets) {
            try {
                s.close();
            } catch (IOException e) {
            }
        }
        sockets.clear();
    }
    
    public synchronized void shutdown() {
        try {
            socket.close();
        } catch (IOException e) {
        }
        dropConnections();
    }
    
    private void startAcceptor() {
        Thread a = new Thread() {
            public void run() {accept();};
        };
        a.setDaemon(true);
        a.setName("Proxy.Acceptor");
        a.start();
    }

    protected void accept() {
        try {
            while(true) {
                Socket iconn = socket.accept();
                System.out.println("PROXY: Accept connection");
                synchronized(this) {
                    sockets.add(iconn);
                }
                Socket oconn = new Socket();
                oconn.setSoLinger(true, 1);
                oconn.connect(new InetSocketAddress(targetAddress.getHostName(), targetAddress.getPort()));
                System.out.println("PROXY: Connected to taget");
                synchronized(this) {
                    sockets.add(oconn);
                    doStream("Proxy-OUT", iconn.getInputStream(), oconn.getOutputStream());
                    doStream("Proxy-IN", oconn.getInputStream(), iconn.getOutputStream());
                }
            }
        }
        catch(IOException e) {
            // ignore;
            e.printStackTrace();
            shutdown();
        }        
    }

    private void doStream(String name, final InputStream inputStream, final OutputStream outputStream) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                stream(inputStream, outputStream);
            }
        };
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
    }

    protected void stream(InputStream inputStream, OutputStream outputStream) {
        byte[] buffer = new byte[64 << 10];
        try {
            while(true) {
                int n = inputStream.read(buffer);
                if (n < 0) {
                    outputStream.close();
                    break;
                }
                System.out.println(Thread.currentThread().getName() + " Sending " + n + " bytes");
                outputStream.write(buffer, 0, n);
            }
        }
        catch(IOException e) {
            try {
                inputStream.close();
            } catch (IOException ee) {
            }
            try {
                outputStream.close();
            } catch (IOException ee) {
            }
        }
    }
}
