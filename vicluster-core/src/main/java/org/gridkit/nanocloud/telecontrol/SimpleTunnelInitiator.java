package org.gridkit.nanocloud.telecontrol;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.nanocloud.telecontrol.HostControlConsole.Destroyable;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.ClasspathUtils;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.StreamCopyService.Link;
import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.gridkit.zeroio.LineLoggerOutputStream;
import org.gridkit.zerormi.zlog.ZLogger;

public class SimpleTunnelInitiator implements TunnellerInitiator {
	
	private String javaCmd;
	private ZLogger logger;
	private StreamCopyService streamCopyService;

	public SimpleTunnelInitiator(String javaCmd, String fileCachePath, StreamCopyService streamCopyService, ZLogger logger) {
		this.javaCmd = javaCmd;
		this.streamCopyService = streamCopyService;
		this.logger = logger;
	}

	@Override
	public HostControlConsole initTunnel(HostControlConsole console) {

		String jversion = getJavaVersion(console);
		if (jversion != null) {
			verifyVersion(jversion);
			logger.debug().log("Host JVM version is " + jversion);
		}
		
		byte[] bootJar;
		try {
			bootJar = ClasspathUtils.createBootstrapperJar(null, Tunneller.class);
		} catch (IOException e1) {
			throw new RuntimeException("Failed to build tunneller.jar", e1);
		}
		String jarpath = console.cacheFile(Classpath.createBinaryEntry("tunneller.jar", bootJar));
		String cachePath = detectCachePath(jarpath);

		final FutureBox<TunnellerConnection> tc = new FutureBox<TunnellerConnection>();
		
		ProcessHandler th = new ProcessHandler() {
			
			Link diag;
			
			@Override
			public void started(final OutputStream stdIn, final InputStream stdOut, InputStream stdErr) {
				final LineLoggerOutputStream log = new LineLoggerOutputStream("", logger.getLogger("console").warn());
				final LineLoggerOutputStream dlog = new LineLoggerOutputStream("", logger.getLogger("tunneller").info());
				diag = streamCopyService.link(stdErr, log, true);
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							// TODO blocking operation in console control thread may be an issue
							TunnellerConnection tcon = new TunnellerConnection("tunneller", stdOut, stdIn, new PrintStream(dlog), 10, TimeUnit.SECONDS);
							tc.setData(tcon);
						} catch (IOException e) {
							tc.setError(e);
						} catch (InterruptedException e) {
							tc.setError(e);
						} catch (TimeoutException e) {
							tc.setError(e);
						}
						diag.flush();						
					}
				});
				thread.setName("Tunnel initializer");
				thread.start();
			}
			
			@Override
			public void finished(int exitCode) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
				diag.flush();
				tc.setErrorIfWaiting(new RuntimeException("Tunneller exit code: " + exitCode));
			}
		};
		
		Destroyable proc = console.startProcess(null, tunnellerCommand(jarpath), null, th);
		TunnellerConnection conn;
		conn = fget(tc, proc);
		
		return new CosnoleWrapper(new TunnellerControlConsole(conn, cachePath), proc);
	}

	private void verifyVersion(String jversion) {
		String[] split = jversion.split("[.]");
		if (split.length < 2) {
			throw new IllegalArgumentException("Unsupported remote Java version: " + jversion);
		}
		int major = toInt(split[0]);
		int minor = toInt(split[1]);
		if (major < 1 || minor < 6) {
			throw new IllegalArgumentException("Unsupported remote Java version: " + jversion);
		}
	}

	private int toInt(String n) {
		try {
			return Integer.parseInt(n);
		}
		catch(NumberFormatException e) {
			return -1;
		}
	}

	private String[] tunnellerCommand(String jarpath) {
	    List<String> cmd = new ArrayList<String>();
	    if (javaCmd.indexOf('|') >= 0) {
	        cmd.addAll(Arrays.asList(javaCmd.split("\\|")));
	    }
	    else {
	        cmd.add(javaCmd);
	    }
	    cmd.addAll(Arrays.asList("-Xmx32m", "-Xms32m", "-cp", jarpath, Tunneller.class.getName()));
		return cmd.toArray(new String[cmd.size()]);
	}

	private String detectCachePath(String jarpath) {
		String cachePath = jarpath;
		if (cachePath.indexOf('/') >= 0) {
			cachePath =cachePath.substring(0, cachePath.lastIndexOf('/') + 1);
		}
		if (cachePath.indexOf('\\') >= 0) {
			cachePath =cachePath.substring(0, cachePath.lastIndexOf('\\') + 1);
		}
		cachePath = cachePath + "..";
		return cachePath;
	}

	private static <T> T fget(Future<T> f, Destroyable cleanUp) {
		try {
			return f.get();
		} catch (InterruptedException e) {
			if (cleanUp != null) {
				cleanUp.destroy();
			}
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			if (cleanUp != null) {
				cleanUp.destroy();
			}
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)(e.getCause());
			}
			else if (e.getCause() instanceof Error) {
				throw (Error)(e.getCause());
			} 
			else {
				throw new RuntimeException("Failed to start remote process", e.getCause());
			}
		}
	}
	
	private static class CosnoleWrapper implements HostControlConsole {
		
		private final HostControlConsole delegate;
		private final Destroyable destroyable;
		
		public CosnoleWrapper(HostControlConsole delegate,	Destroyable destroyable) {
			this.delegate = delegate;
			this.destroyable = destroyable;
		}
		
		@Override
		public boolean isLocalFileSystem() {
			return false;
		}

		public String cacheFile(FileBlob blob) {
			return delegate.cacheFile(blob);
		}

		public List<String> cacheFiles(List<? extends FileBlob> blobs) {
			return delegate.cacheFiles(blobs);
		}

		public Destroyable openSocket(SocketHandler handler) {
			return delegate.openSocket(handler);
		}

		public Destroyable startProcess(String workDir, String[] command, Map<String, String> env, ProcessHandler handler) {
			return delegate.startProcess(workDir, command, env, handler);
		}

		@Override
		public void terminate() {
			delegate.terminate();
			destroyable.destroy();
		}
	}
	
	
	@SuppressWarnings("resource")
    private String getJavaVersion(HostControlConsole console) {
		try {
			final FutureBox<Void> done = new FutureBox<Void>();
			final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
			final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

			ProcessHandler handler = new ProcessHandler() {

				Link lout;
				Link lerr;
				
				@Override
				public void started(OutputStream stdIn, InputStream out, InputStream err) {
					try {
						stdIn.close();
					} catch (IOException e) {
						// ignore
					}
					lout = streamCopyService.link(out, stdOut);
					lerr = streamCopyService.link(err, stdErr);
				}
				
				@Override
				public void finished(int exitCode) {
					lout.flushAndClose();
					lerr.flushAndClose();
					done.setData(null);
				}
			};
			
			console.startProcess(null, new String[]{javaCmd, "-version"}, null, handler);
			try {
				done.get();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
			
			BufferedReader outr = new BufferedReader(new StringReader(new String(stdOut.toByteArray()))); 
			BufferedReader errr = new BufferedReader(new StringReader(new String(stdErr.toByteArray()))); 
			
			Pattern p = Pattern.compile("(openjdk|java) version \"([^\"]*)\"");
			
			while(true) {
				String line = errr.readLine();
				if (line == null) {
					break;
				}
				Matcher m = p.matcher(line);
				if (m.matches()) {
					return m.group(2);
				}
				logger.critical().log("{java -version} " + line);
			}
			
			logger.fatal().log("JVM verification failed: " + javaCmd);
			while(true) {
				String line = outr.readLine();
				if (line == null) {
					break;
				}
				logger.critical().log("{java -version} " + line);
			}
			
			return null;
		} catch (IOException e) {
			logger.warn().log("JVM verification error", e);
			return null;
		}
	}
}
