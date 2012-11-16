package org.gridkit.vicluster.telecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.ExecHandler;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.SocketHandler;
import org.junit.Test;

public class TunnellerProtocolTest {

	private InputStream masterIn;
	private OutputStream masterOut;
	
	private InputStream slaveIn;
	private OutputStream slaveOut;
	
	private Tunneller tunneler;
	private TunnellerConnection connection;
	
	public void start() throws IOException {
		StreamPipe pipeA = new StreamPipe(64 << 10);
		StreamPipe pipeB = new StreamPipe(64 << 10);
		masterIn = new ISW("masterIn", pipeA.getInputStream());
		slaveOut = new OSW("slaveOut", pipeA.getOutputStream());
		masterOut = new OSW("masterOut", pipeB.getOutputStream());
		slaveIn = new ISW("slaveIn", pipeB.getInputStream());
		
		tunneler = new Tunneller();
		new Thread("Slave") {
			
			public void run() {
				tunneler.process(slaveIn, slaveOut);
			}
		}.start();
		connection = new TunnellerConnection("TEST", masterIn, masterOut);
		
//		TunnelTestHelper.enableChannelTrace(tunneler);
//		TunnelTestHelper.enableChannelTrace(connection);
	}
	
	@Test
	public void test_vanila_exec() throws InterruptedException, ExecutionException, IOException {
		start();
		
		FutureBox<Void> done = exec("echo", "Hallo welt!");
		
		done.get();
	}

	@Test
	public void test_exec_with_stdErr() throws IOException, InterruptedException, ExecutionException {
		start();
		
		FutureBox<Void> done = execCmd("echo \"Hallo welt!\" 1>&2\n");
		
		done.get();
		
	}
	
	@Test 
	public void test_exec_resource_leak() throws IOException, InterruptedException, ExecutionException {
		start();
		
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		for(int i = 0; i != 1000; ++i) {
			futures.add(exec("echo", "exec-" + String.valueOf(i)));
			if (i > 4) {
				futures.remove(0).get();
			}
		}
		for(Future<Void> f: futures) {
			f.get();
		}		
	}
	
	@Test
	public void test_bind() throws IOException, InterruptedException, ExecutionException {
		start();
		
		final FutureBox<SocketAddress> bind = new FutureBox<SocketAddress>();
		
		connection.newSocket(new SocketHandler() {
			
			@Override
			public void bound(String host, int port) {
				bind.setData(new InetSocketAddress(host, port));
			}
			
			@Override
			public void accepted(String rhost, int rport, InputStream soIn, OutputStream soOut) {
				System.out.println("Accepted [" + rhost + ": " + rport + "]");
				try {
					soOut.write("Pong!\n".getBytes());
					StreamHelper.copy(soIn, System.out);
					soIn.close();
					soOut.close();
					System.out.println("Remote socket closed");
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		});
		
		SocketAddress sa = bind.get();
		System.out.println("Bound socket: " + sa);
		Socket sock = new Socket();
		sock.connect(sa);
		sock.getOutputStream().write("Ping!\n".getBytes());
//		sock.shutdownOutput();
		Thread.sleep(500);
		StreamHelper.copyAvailable(sock.getInputStream(), System.err);
		Thread.sleep(500);
		sock.close();
		System.out.println("Socket closed");
		
		Assert.assertTrue(sock.isClosed());
		Thread.sleep(1000000);
	}
	
	private FutureBox<Void> exec(String... cmd) throws IOException {
		final FutureBox<Void> done = new FutureBox<Void>();
		
		connection.exec(".", cmd, new String[0], new ExecHandler() {
			
			InputStream stdOut;
			InputStream stdErr;
			
			@Override
			public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
				try {
					stdIn.close();
				} catch (IOException e) {
					// ingore
				}
				this.stdOut = stdOut;
				this.stdErr = stdErr;
			}
			
			@Override
			public void finished(int exitCode) {
				try {
					StreamHelper.copy(stdOut, System.out);
					StreamHelper.copy(stdErr, System.err);
					System.out.println("Exit code " + exitCode);
					done.setData(null);
				} catch (IOException e) {
					done.setError(e);
				}
			}
		});
		return done;
	}
	
	private FutureBox<Void> execCmd(final String cmd) throws IOException {
		final FutureBox<Void> done = new FutureBox<Void>();
		
		String sh = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase().startsWith("windows") ? "cmd" : "sh";
		
		connection.exec(".", new String[]{sh}, new String[0], new ExecHandler() {
			
			InputStream stdOut;
			InputStream stdErr;
			
			@Override
			public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
				System.out.println("Started");
				try {
					stdIn.write(cmd.getBytes());
					stdIn.close();
				} catch (IOException e) {
					// ingore
				}
				this.stdOut = stdOut;
				this.stdErr = stdErr;
			}
			
			@Override
			public void finished(int exitCode) {
				try {
					StreamHelper.copy(stdOut, System.out);
					StreamHelper.copy(stdErr, System.err);
					System.out.println("Exit code " + exitCode);
					done.setData(null);
				} catch (IOException e) {
					done.setError(e);
				}
			}
		});
		return done;
	}

	@SuppressWarnings("unused")
	private FutureBox<Void> execCat(final String cmd) throws IOException {
		final FutureBox<Void> done = new FutureBox<Void>();
		
		String sh = "cat";
		
		connection.exec(".", new String[]{sh}, new String[0], new ExecHandler() {
			
			InputStream stdOut;
			InputStream stdErr;
			
			@Override
			public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
				System.out.println("Started");
				try {
					stdIn.write(cmd.getBytes());
					stdIn.close();
				} catch (IOException e) {
					// ingore
				}
				this.stdOut = stdOut;
				this.stdErr = stdErr;
			}
			
			@Override
			public void finished(int exitCode) {
				try {
					StreamHelper.copy(stdOut, System.out);
					StreamHelper.copy(stdErr, System.err);
					System.out.println("Exit code " + exitCode);
					done.setData(null);
				} catch (IOException e) {
					done.setError(e);
				}
			}
		});
		return done;
	}
	
	private static class OSW extends OutputStream {
		
		private String name;
		private OutputStream delegate;
		
		public OSW(String name, OutputStream delegate) {
			this.name = name;
			this.delegate = delegate;
		}

		public void write(int b) throws IOException {
			delegate.write(b);
		}

		public void write(byte[] b) throws IOException {
			delegate.write(b);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			delegate.write(b, off, len);
		}

		public void flush() throws IOException {
			delegate.flush();
		}

		public void close() throws IOException {
			delegate.close();
			System.out.println("Closed stream: " + name);
		}

		public String toString() {
			return delegate.toString();
		}
	}
	
	private static class ISW extends InputStream {
		
		private String name;
		private InputStream delegate;
		
		public ISW(String name, InputStream delegate) {
			this.name = name;
			this.delegate = delegate;
		}

		public int read() throws IOException {
			return delegate.read();
		}

		public int read(byte[] b) throws IOException {
			return delegate.read(b);
		}

		public int read(byte[] b, int off, int len) throws IOException {
			return delegate.read(b, off, len);
		}

		public long skip(long n) throws IOException {
			return delegate.skip(n);
		}

		public int available() throws IOException {
			return delegate.available();
		}

		public String toString() {
			return delegate.toString();
		}

		public void close() throws IOException {
			delegate.close();
			System.out.println("Closed stream: " + name);
		}

		public void mark(int readlimit) {
			delegate.mark(readlimit);
		}

		public void reset() throws IOException {
			delegate.reset();
		}

		public boolean markSupported() {
			return delegate.markSupported();
		}
	}
}
