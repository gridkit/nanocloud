package org.gridkit.zerormi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;


public class EnvelopStreamTest {

    StreamPipe pipe;
    EnvelopInputStream in;
    EnvelopOutputStream out;
    DataInputStream din;
    DataOutputStream dout;
        
    public void init(int pipeBuffer) {
        pipe = new StreamPipe(pipeBuffer);
        in = new EnvelopInputStream(pipe.getInputStream());
        out = new EnvelopOutputStream(pipe.getOutputStream());
        din = new DataInputStream(in);
        dout = new DataOutputStream(out);
    }
    
    @Before
    public void init() {
        init(64 << 10);
    }
    
    @Test
    public void one_message() throws IOException {
        dout.writeUTF("Hallo");
        out.closeMessage();
        
        String m1 = din.readUTF();
        assertEquals("Hallo", m1);
        assertEquals("EOF expected", -1, in.read());
    }

    @Test
    public void two_messages() throws IOException {
        dout.writeUTF("Hallo");
        out.closeMessage();
        dout.writeUTF("pipe");
        out.closeMessage();
        
        String m1 = din.readUTF();
        assertEquals("Hallo", m1);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        String m2 = din.readUTF();
        assertEquals("EOF expected", -1, in.read());
        assertEquals("pipe", m2);
        in.nextMessage();        
    }

    @Test
    public void test_large_messages() throws IOException {
        dout.writeUTF(bigString('a', 15 << 10));
        out.closeMessage();
        dout.writeUTF(bigString('A', 17 << 10));
        out.closeMessage();
        
        String m1 = din.readUTF();
        assertEquals(bigString('a', 15 << 10), m1);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        String m2 = din.readUTF();
        assertEquals("EOF expected", -1, in.read());
        assertEquals(bigString('A', 17 << 10), m2);
        in.nextMessage();        
    }

    @Test
    public void offset_read_and_write() throws IOException {
        byte[] data = bigData(13, 16 << 10);
        int l1 = 10000;
        int l2 = 333;
        int l3 = data.length - l1 - l2;
        
        out.write(data, 0, l1);
        out.closeMessage();
        out.write(data, l1, l2);
        out.closeMessage();
        out.write(data, l1 + l2, l3);
        out.closeMessage();

        byte[] data2 = new byte[32 << 10];
        int n = 1000;
        n += in.read(data2, n, l1);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        n += in.read(data2, n, l2);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        n += in.read(data2, n, l3);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();

        assertEquals(data.length, n - 1000);
        
        byte[] data3  = Arrays.copyOfRange(data2, 1000, 1000 + data.length);
        assertArrayEquals(data, data3);
    }

    @Test
    public void offset_read_write_skip() throws IOException {
        byte[] data = bigData(13, 16 << 10);
        int l1 = 10000;
        int l2 = 333;
        int l3 = data.length - l1 - l2;
        
        out.write(data, 0, l1);
        out.closeMessage();
        out.write(data, l1, l2);
        out.closeMessage();
        out.write(data);
        out.closeMessage();
        out.write(data, l1 + l2, l3);
        out.closeMessage();
        
        byte[] data2 = new byte[32 << 10];
        int n = 1000;
        n += in.read(data2, n, l1);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        n += in.read(data2, n, l2);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        in.skip(Long.MAX_VALUE);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        n += in.read(data2, n, l3);
        assertEquals("EOF expected", -1, in.read());
        in.nextMessage();
        
        assertEquals(data.length, n - 1000);
        
        byte[] data3  = Arrays.copyOfRange(data2, 1000, 1000 + data.length);
        assertArrayEquals(data, data3);
    }
    
    private static String bigString(char ch, int len) {
        char[] b = new char[len];
        for(int i = 0; i != b.length; ++i) {
            b[i] = (char)(ch + (i % 7));
        }
        return new String(b);
    }

    private static byte[] bigData(int ch, int len) {
        byte[] b = new byte[len];
        for(int i = 0; i != b.length; ++i) {
            b[i] = (byte)(ch + (i % 7));
        }
        return b;
    }
}
