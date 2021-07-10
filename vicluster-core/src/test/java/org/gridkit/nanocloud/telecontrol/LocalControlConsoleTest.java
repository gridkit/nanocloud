package org.gridkit.nanocloud.telecontrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.SocketHandler;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class LocalControlConsoleTest  {

    @Rule
    public Timeout timeout = new Timeout(60 * 1000);

    protected HostControlConsole console;

    @Before
    public void initConsole() throws IOException, InterruptedException, TimeoutException {
        console = new LocalControlConsole("{tmp}/nanocloud-console-test");
    }

    @After
    public void destroyConsole() {
        console.terminate();
        if (console instanceof LocalControlConsole)
        rmrf(((LocalControlConsole) console).getCacheDir());
    }

    private static void rmrf(File file) {
        if (file.isFile()) {
            file.delete();
        }
        else if (file.isDirectory()) {
            File[] c = file.listFiles();
            if (c != null) {
                for(File f: c) {
                    rmrf(f);
                }
            }
            file.delete();
        }
    }

    @Test
    public void verify_classpath_replication() {

        List<ClasspathEntry> cp = Classpath.getClasspath(getClass().getClassLoader());
        System.out.println("Cached classpath");
        for(ClasspathEntry entry: cp) {
            String path = console.cacheFile(entry);
            System.out.println(" - " + path);
            File file = new File(path);
            assertTrue(file.isFile());
            assertEquals("File size should be same", entry.size(), file.length());
        }
    }

    @Test
    public void verify_ephemeral_data_caching() {

        ByteBlob blob1 = new ByteBlob("test-blob", "1234".getBytes());
        ByteBlob blob2 = new ByteBlob("test-blob", "ABC".getBytes());
        ByteBlob blob3 = new ByteBlob("test-blob", new byte[0]);

        String path1 = console.cacheFile(blob1);
        String path2 = console.cacheFile(blob2);
        String path3 = console.cacheFile(blob3);

        assertTrue(new File(path1).isFile());
        assertEquals("File size ", 4, new File(path1).length());

        assertTrue(new File(path2).isFile());
        assertEquals("File size ", 3, new File(path2).length());

        assertTrue(new File(path3).isFile());
        assertEquals("File size ", 0, new File(path3).length());
    }

    private byte[] generateData(int seed) {
        byte[] data = new byte[(30 << 10) + seed];
        Random rnd = new Random(seed);
        rnd.nextBytes(data);
        return data;
    }

    @Test
    public void verify_bulk_file_transfer() {

        int fileCount = 20;
        List<String> paths = new ArrayList<String>();

        for(int i = 0; i != fileCount; ++i) {
            ByteBlob blob = new ByteBlob("test-blob-" + i, generateData(i));
            paths.add(console.cacheFile(blob));
        }

        for(int i = 0; i != fileCount; ++i) {
            String path = paths.get(i);
            assertTrue(new File(path).isFile());
            assertEquals("File size ", (30 << 10) + i, new File(path).length());
        }
    }

    @Test
    public void verify_bulk_file_transfer_parallel() {

        int fileCount = 100;
        List<ByteBlob> blobs = new ArrayList<ByteBlob>();
        List<String> paths = new ArrayList<String>();

        for(int i = 0; i != fileCount; ++i) {
            ByteBlob blob = new ByteBlob("test-blob-" + i, generateData(i));
            blobs.add(blob);
        }

        paths.addAll(console.cacheFiles(blobs));

        for(int i = 0; i != fileCount; ++i) {
            String path = paths.get(i);
            assertTrue(new File(path).isFile());
            assertEquals("File size ", (30 << 10) + i, new File(path).length());
        }
    }

    @Test
    public void verify_bulk_transfer_half_cached() {

        int fileCount = 20;
        List<String> paths = new ArrayList<String>();

        for(int i = 0; i != fileCount; ++i) {
            if (i % 2 == 0) {
                ByteBlob blob = new ByteBlob("test-blob-" + i, generateData(i));
                console.cacheFile(blob);
            }
        }

        for(int i = 0; i != fileCount; ++i) {
            ByteBlob blob = new ByteBlob("test-blob-" + i, generateData(i));
            paths.add(console.cacheFile(blob));
        }

        for(int i = 0; i != fileCount; ++i) {
            String path = paths.get(i);
            assertTrue(new File(path).isFile());
            assertEquals("File size ", (30 << 10) + i, new File(path).length());
        }
    }

    @Test
    public void verify_bulk_transfer_half_cached_parallel() {

        int fileCount = 100;
        List<ByteBlob> blobs = new ArrayList<ByteBlob>();
        List<String> paths = new ArrayList<String>();

        for(int i = 0; i != fileCount; ++i) {
            if (i % 2 == 0) {
                ByteBlob blob = new ByteBlob("test-blob-" + i, generateData(i));
                blobs.add(blob);
            }
        }

        console.cacheFiles(blobs);
        blobs.clear();

        for(int i = 0; i != fileCount; ++i) {
            ByteBlob blob = new ByteBlob("test-blob-" + i, generateData(i));
            blobs.add(blob);
        }

        paths.addAll(console.cacheFiles(blobs));

        for(int i = 0; i != fileCount; ++i) {
            String path = paths.get(i);
            assertTrue(new File(path).isFile());
            assertEquals("File size ", (30 << 10) + i, new File(path).length());
        }
    }

    @Test
    public void verify_content_addressing() {

        ByteBlob blob1 = new ByteBlob("test-blob", "1234".getBytes());
        ByteBlob blob2 = new ByteBlob("test-blob", "1234".getBytes());
        ByteBlob blob3 = new ByteBlob("another-blob", "1234".getBytes());
        ByteBlob blob4 = new ByteBlob("test-blob", new byte[0]);

        String path1 = console.cacheFile(blob1);
        String path2 = console.cacheFile(blob2);
        String path3 = console.cacheFile(blob3);
        String path4 = console.cacheFile(blob4);

        assertEquals("Blobs with same content", path1, path2);
        assertFalse("Blobs with same content, but different name", path1.equals(path3));
        assertFalse("Blobs with dufferent content, but same name", path1.equals(path4));
    }

    @Test
    public void verify_jvm_execution() throws IOException, InterruptedException, ExecutionException {

        String javaHome = System.getProperty("java.home");

        final ByteArrayOutputStream pout = new ByteArrayOutputStream();
        final ByteArrayOutputStream perr = new ByteArrayOutputStream();

        final FutureBox<Integer> pexit = new FutureBox<Integer>();

        String exec = new File(new File(new File(javaHome), "bin"), "java").getAbsolutePath();

        ProcessHandler handler = new ProcessHandler() {

            @Override
            public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
                try {
                    stdIn.close();
                    StreamHelper.copy(stdOut, pout);
                    StreamHelper.copy(stdErr, perr);
                } catch (IOException e) {
                    // ignore
                }
            }

            @Override
            public void execFailed(OutputStream stdIn, InputStream stdOut, InputStream stdErr, String error) {
                // mimic old event sequence
                started(stdIn, stdOut, stdErr);
                finished(Integer.MIN_VALUE);
            }

            @Override
            public void finished(int exitCode) {
                pexit.setData(exitCode);
            }
        };

        console.startProcess(".", new String[]{exec, "-version"}, null, handler);

        assertEquals("Exit code", 0, pexit.get().intValue());
        System.out.write(pout.toByteArray());
        System.out.write(perr.toByteArray());
    }

    @Test
    public void verify_execution_failure() throws IOException, InterruptedException, ExecutionException {

        String javaHome = System.getProperty("java.home");

        final ByteArrayOutputStream pout = new ByteArrayOutputStream();
        final ByteArrayOutputStream perr = new ByteArrayOutputStream();

        final FutureBox<Integer> pexit = new FutureBox<Integer>();

        String exec = new File(new File(new File(javaHome), "bin"), "java").getAbsolutePath();
        exec += ".no-such-file";

        ProcessHandler handler = new ProcessHandler() {

            @Override
            public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
                try {
                    stdIn.close();
                    StreamHelper.copy(stdOut, pout);
                    StreamHelper.copy(stdErr, perr);
                } catch (IOException e) {
                    // ignore
                }
            }

            @Override
            public void execFailed(OutputStream stdIn, InputStream stdOut, InputStream stdErr, String error) {
                // mimic old event sequence
                started(stdIn, stdOut, stdErr);
                finished(Integer.MIN_VALUE);
            }

            @Override
            public void finished(int exitCode) {
                pexit.setData(exitCode);
            }
        };

        console.startProcess(".", new String[]{exec, "-version"}, null, handler);

        assertEquals("Exit code", Integer.MIN_VALUE, pexit.get().intValue());
        System.out.write(pout.toByteArray());
        System.err.write(perr.toByteArray());
    }

    @Test
    public void verify_tunneled_connection() throws IOException, InterruptedException, ExecutionException {

        final FutureBox<SocketAddress> bindAddress = new FutureBox<SocketAddress>();
        final FutureBox<SocketAddress> clientAddress = new FutureBox<SocketAddress>();

        SocketHandler sockHandler = new SocketHandler() {

            @Override
            public void bound(String host, int port) {
                System.out.println("Bound: " + host + ":" + port);
                bindAddress.setData(new InetSocketAddress(host, port));
            }

            @Override
            public void accepted(String remoteHost, int remotePort, final InputStream soIn, final OutputStream soOut) {
                System.out.println("Connected: " + remoteHost + ":" + remotePort);
                clientAddress.setData(new InetSocketAddress(remoteHost, remotePort));
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            soOut.write("Ping".getBytes());
                            soOut.flush();
                            soOut.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }

            @Override
            public void terminated(String message) {
                System.out.println(message);
            }
        };

        console.openSocket(sockHandler);

        Socket sock = new Socket();
        sock.connect(bindAddress.get());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamHelper.copy(sock.getInputStream(), bos);

        assertEquals(((InetSocketAddress)clientAddress.get()).getPort(), sock.getLocalPort());
        assertEquals("Ping", new String(bos.toByteArray()));
    }

    // TODO should support disposable sockets
    @Test
    //@Ignore("See TODO")
    public void verify_multi_bind_tunneled_connection() throws IOException, InterruptedException, ExecutionException {

        final FutureBox<SocketAddress> bindAddress = new FutureBox<SocketAddress>();

        SocketHandler sockHandler = new SocketHandler() {

            @Override
            public void bound(String host, int port) {
                System.out.println("Bound: " + host + ":" + port);
                bindAddress.setData(new InetSocketAddress(host, port));
            }

            @Override
            public void accepted(String remoteHost, int remotePort, final InputStream soIn, final OutputStream soOut) {
                System.out.println("Connected: " + remoteHost + ":" + remotePort);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            soOut.write("Ping".getBytes());
                            soOut.flush();
                            soOut.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }

            @Override
            public void terminated(String message) {
                System.out.println(message);
            }
        };

        console.openSocket(sockHandler);

        System.out.println("Connecting first time");
        Socket sock = new Socket();
        sock.connect(bindAddress.get());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamHelper.copy(sock.getInputStream(), bos);
        assertEquals("Ping", new String(bos.toByteArray()));

        // Connecting one more time

        System.out.println("Connecting second time");
        sock = new Socket();
        sock.connect(bindAddress.get());

        bos = new ByteArrayOutputStream();
        StreamHelper.copy(sock.getInputStream(), bos);
        assertEquals("Ping", new String(bos.toByteArray()));
    }

    static class ByteBlob implements FileBlob {

        private String filename;
        private String hash;
        private byte[] data;

        public ByteBlob(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
            this.hash = StreamHelper.digest(data, "SHA-1");
        }

        @Override
        public File getLocalFile() {
            return null;
        }

        @Override
        public String getFileName() {
            return filename;
        }

        @Override
        public String getContentHash() {
            return hash;
        }

        @Override
        public InputStream getContent() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public long size() {
            return data.length;
        }
    }
}
