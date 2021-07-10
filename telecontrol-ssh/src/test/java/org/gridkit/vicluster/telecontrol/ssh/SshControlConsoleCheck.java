package org.gridkit.vicluster.telecontrol.ssh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.SocketHandler;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class SshControlConsoleCheck  {

	@Rule
	public Timeout timeout = new Timeout(60 * 1000);
	
	protected HostControlConsole console;
	
	@Before
	public void initConsole() throws IOException, InterruptedException, TimeoutException, JSchException {
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("root");
		sshFactory.setPassword("reverse");
		
		Session session = sshFactory.getSession("cbox1", null);
		
		console= new SshHostControlConsole(session, "~/.nanocloud/cache", false, 1);
	}

	@After
	public void destroyConsole() {
		console.terminate();
	}
	
	@Test
	public void verify_classpath_replication() {
		
		List<ClasspathEntry> cp = Classpath.getClasspath(getClass().getClassLoader());
		System.out.println("Cached classpath");
		for(ClasspathEntry entry: cp) {
			String path = console.cacheFile(entry);
			System.out.println(" - " + path);
		}
	}

	@Test
	public void verify_ephemeral_data_caching() {
		
		ByteBlob blob1 = new ByteBlob("test-blob", "1234".getBytes());
		ByteBlob blob2 = new ByteBlob("test-blob", "ABC".getBytes());
		ByteBlob blob3 = new ByteBlob("test-blob", new byte[0]);
		
		console.cacheFile(blob1);
		console.cacheFile(blob2);
		console.cacheFile(blob3);		
	}

	@Test
	public void verify_content_addressing() {
		
		ByteBlob blob1 = new ByteBlob("test-blob", "1234".getBytes());
		ByteBlob blob2 = new ByteBlob("test-blob", "1234".getBytes());
		ByteBlob blob3 = new ByteBlob("another-blob", "1234".getBytes());
		ByteBlob blob4 = new ByteBlob("test-blob", new byte[0]);
		
		String path1 = console.cacheFile(blob1);
		String path2 = console.cacheFile(blob2);
		@SuppressWarnings("unused")
		String path3 = console.cacheFile(blob3);
		String path4 = console.cacheFile(blob4);
		
		assertEquals("Blobs with same content", path1, path2);
		// SFTP cache ignores content collision case
//		assertFalse("Blobs with same content, but different name", path1.equals(path3));
		assertFalse("Blobs with dufferent content, but same name", path1.equals(path4));
	}

	@Test
	public void verify_jvm_execution() throws IOException, InterruptedException, ExecutionException {
		
		final ByteArrayOutputStream pout = new ByteArrayOutputStream();
		final ByteArrayOutputStream perr = new ByteArrayOutputStream();

		final FutureBox<Integer> pexit = new FutureBox<Integer>();
		
		String exec = "java";
		
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
		
		final ByteArrayOutputStream pout = new ByteArrayOutputStream();
		final ByteArrayOutputStream perr = new ByteArrayOutputStream();
		
		final FutureBox<Integer> pexit = new FutureBox<Integer>();
		
		String exec = "no-such-file";
		
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
		
		assertEquals("Exit code", 127, pexit.get().intValue());
		System.out.write(pout.toByteArray());
		System.err.write(perr.toByteArray());
	}
	
	@Test 
	@Ignore("Not implemented so far")
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

	@Test 
	@Ignore("Not implemented so far")
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
