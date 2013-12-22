package org.gridkit.nanocloud.telecontrol.isolate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViSpiConfig;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.ClasspathUtils;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamPipe;
import org.gridkit.zeroio.LookbackOutputStream;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.NamedStreamPair;
import org.gridkit.zerormi.hub.SlaveSpore;

class IsolateLauncher implements ProcessLauncher {

	@Override
	public ManagedProcess createProcess(Map<String, Object> config) {
		
		ViSpiConfig ctx = ViEngine.Core.asSpiConfig(config);
		RemoteExecutionSession rmiSession = ctx.getRemotingSession();
		List<Classpath.ClasspathEntry> cp = ctx.getSlaveClasspath();
		
		List<URL> urls = new ArrayList<URL>();
		for(Classpath.ClasspathEntry ce: cp) {
			urls.add(ce.getUrl());
		}
		
		ClassLoader cl = ClasspathUtils.getNearestSystemClassloader(Thread.currentThread().getContextClassLoader());
		if (cl == null) {
			// TODO this most likely a bug, add "secret" property to override this issue
			throw new RuntimeException("Library classloader is not found!");
		}
		
		Isolate i = new Isolate(ctx.getNodeName(), cl, urls);
		
		IsolateSession session = new IsolateSession(i, rmiSession);
		session.start();
		
		return session;
	}

	private static class IsolateSession implements ManagedProcess {

		private Isolate isolate;
		private RemoteExecutionSession session;
		private LookbackOutputStream stdOut = new LookbackOutputStream(4096);
		private LookbackOutputStream stdErr = new LookbackOutputStream(4096);
		
		private AdvancedExecutor executor;
		private FutureBox<Integer> exitBox = new FutureBox<Integer>();
		
		public IsolateSession(Isolate isolate, RemoteExecutionSession session) {
			this.isolate = isolate;
			this.session = session;
			this.isolate.replaceSdtOut(new PrintStream(stdOut));
			this.isolate.replaceSdtErr(new PrintStream(stdErr));
		}
		
		public void start() {
			session.setTransportConnection(bootstrap(session.getMobileSpore()));
			executor = session.getRemoteExecutor();
		}
		
		private DuplexStream bootstrap(final SlaveSpore spore) {
			StreamPipe controlIn = new StreamPipe(4096);
			StreamPipe controlOut = new StreamPipe(4096);
			
			final OutputStream sout = controlOut.getOutputStream();
			final InputStream sin = controlIn.getInputStream();

			DuplexStream outter = new NamedStreamPair("[" + isolate.getName() + "]:external", controlOut.getInputStream(), controlIn.getOutputStream());
			
			isolate.start();
			
			isolate.execNoMarshal(new Runnable() {
				@Override
				public void run() {
					Bootstrapper bs = new Bootstrapper(isolate.getName(), spore);
					@SuppressWarnings("deprecation")
					final Runnable ibs = (Runnable) isolate.convertIn(bs);
					bindConnection(ibs, sin, sout);
					Thread thread = new Thread() {
						@Override
						public void run() {
							try {
								ibs.run();
							}
							finally {
								exitBox.setData(0);
							}
						}						
					};
					thread.setName("ISOLATE-BOOT[" + isolate.getName() + "]");
					thread.start();
				}
			});
			
			return outter;
		}
		
		@Override
		public void suspend() {
			isolate.suspend();
		}

		@Override
		public void resume() {
			isolate.suspend();
		}

		@Override
		public void destroy() {
			session.terminate();
			// TODO hardshutdown
		}

		@Override
		public void consoleFlush() {
			try {
				stdOut.flush();
			} catch (IOException e) {
				// ignore
			}
			try {
				stdErr.flush();
			} catch (IOException e) {
				// ignore
			}
		}

		@Override
		public FutureEx<Integer> getExitCodeFuture() {
			return exitBox;
		}

		@Override
		public AdvancedExecutor getExecutionService() {
			return executor;
		}

		@Override
		public void bindStdIn(InputStream is) {
			// TODO impelement stdIn binding for Isolate
		}

		@Override
		public void bindStdOut(OutputStream os) {
			try {
				stdOut.setOutput(os);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void bindStdErr(OutputStream os) {
			try {
				stdErr.setOutput(os);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static void bindConnection(Runnable bootstraper, InputStream is, OutputStream os) {
		try {
			Method m = bootstraper.getClass().getMethod("connect", InputStream.class, OutputStream.class);
			m.setAccessible(true);
			m.invoke(bootstraper, is, os);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("serial")
	static class Bootstrapper implements Runnable, Serializable, DuplexStreamConnector {
		
		private String name;
		private SlaveSpore spore;
		private InputStream in;
		private OutputStream out;
		
		public Bootstrapper(String name, SlaveSpore spore) {
			this.name = name;
			this.spore = spore;
		}

		public void connect(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public DuplexStream connect() throws IOException {
			return new NamedStreamPair("[" + name + "]:internal", in, out);
		}

		@Override
		public void run() {
			spore.start(this);			
		}
	}
}
