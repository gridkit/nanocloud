package org.gridkit.nanocloud.agent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.gridkit.nanocloud.Nanocloud;
import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;

/**
 * This is {@link Nanocloud} explicit agent implementation for plain socket communication.
 * <p>
 * Using SSH agent is deployed automatically. Though non SSH transport require explicit agent deployment.
 * <p>
 * This agent allows inbound traffic on TCP socket.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class PlainSocketNanoAgent {

    public static void main(String[] args) throws InterruptedException {
        Integer port = Integer.getInteger("nanoagent.port");
        if (port == null) {
            throw new IllegalArgumentException("Port is required, add -Dnanoagent.port=<port> option");
        }

        PlainSocketNanoAgent agent = new PlainSocketNanoAgent();
        agent.setPort(port);

        agent.start();
        agent.join();
    }

    private int port;

    private ServerSocket serverSocket;
    private Thread acceptor;

    public void setPort(int port) {
        this.port = port;
    }

    public void start() {

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReceiveBufferSize(Integer.getInteger("nanocloud.tcp.receiveBuffer", 128 << 10));
            serverSocket.setReuseAddress(true);

            System.out.println("Start listening " + serverSocket.getLocalSocketAddress().toString());

            acceptor = new Thread("Accpetor") {
                @Override
                public void run() {
                    accept();
                };
            };

            acceptor.setDaemon(true);
            acceptor.start();

        } catch (IOException e) {
            System.err.println("Failed to start listening on port " + port);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void join() throws InterruptedException {
        acceptor.join();
    }

    private void accept() {
        while (true) {
            try {
                final Socket socket = serverSocket.accept();

                Thread handler = new Thread("Handler-" + socket.getRemoteSocketAddress().toString()) {
                    @Override
                    public void run() {
                        serve(socket);
                    }
                };

                handler.setDaemon(true);
                handler.start();

            } catch (IOException e) {
                System.err.println("New connection error: " + e.toString());
            }
            if (serverSocket.isClosed()) {
                break;
            }
        }
    }

    private void serve(Socket socket) {
        Tunneller tunneler = new Tunneller();
        try {
            tunneler.process(socket.getInputStream(), socket.getOutputStream());
        } catch (Exception e) {
            System.err.println("[" + Thread.currentThread().getName() + "] - fatal error: " + e.toString());
        } finally {
            try {
                tunneler.shutdown();
            } catch (Exception e) {
                // ignore
            }
            try {
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
