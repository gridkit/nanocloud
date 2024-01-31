package org.gridkit.nanocloud.telecontrol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Test;

import junit.framework.Assert;

public class SocketTest {

    @Test
    @SuppressWarnings("resource")
    public void simple_socket_transfer() throws IOException {

        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(null);
        Socket s1 = new Socket();
        s1.connect(ss.getLocalSocketAddress());
        Socket s2 = ss.accept();

        DataOutputStream dos = new DataOutputStream(s1.getOutputStream());
        DataInputStream dis = new DataInputStream(s2.getInputStream());

        dos.writeUTF("Hallo world!");
        String result = dis.readUTF();

        Assert.assertEquals("Hallo world!", result);
    }

    @SuppressWarnings("resource")
    @Test
    public void server_socket_close() throws IOException {

        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(null);
        Socket s1 = new Socket();
        s1.connect(ss.getLocalSocketAddress());
        Socket s2 = ss.accept();

        ss.close();

        DataOutputStream dos = new DataOutputStream(s1.getOutputStream());
        DataInputStream dis = new DataInputStream(s2.getInputStream());

        dos.writeUTF("Hallo world!");
        String result = dis.readUTF();

        Assert.assertEquals("Hallo world!", result);
    }
}
