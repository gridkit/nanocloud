/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.telecontrol.ssh;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.gridkit.internal.com.jcraft.jsch.ChannelExec;
import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.ClasspathUtils;
import org.gridkit.vicluster.telecontrol.ExecCommand;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.bootstraper.Bootstraper;
import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.ExecHandler;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.SocketHandler;
import org.gridkit.vicluster.telecontrol.ssh.LoggerPrintStream.Level;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.NamedStreamPair;
import org.gridkit.zerormi.hub.LegacySpore;
import org.gridkit.zerormi.hub.MasterHub;
import org.gridkit.zerormi.hub.RemotingHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnellerJvmReplicator implements RemoteJmvReplicator {

	private static final long DEFAULT_CONN_TIMEOUT = 5000;
	
	private SshRemotingConfig rconfig = new SshRemotingConfig();
	private boolean initialized;
	private boolean destroyed;
	
	private Session session;
	private RemotingHub hub;
	private TunnellerConnection control;
	
	private RemoteFileCache jarCache;
	private String tunnellerJarPath;
	
	private String tunnelHost;
	private int tunnelPort;
	private long connectTimeoutMS = DEFAULT_CONN_TIMEOUT;
	
	private Logger logger;
	
	@Override
	public synchronized void configure(Map<String, String> nodeConfig) {
		rconfig.configure(nodeConfig);
		rconfig.validate();
	}

	@Override
	public synchronized String getFingerPrint() {
		return rconfig.getFingerPrint();
	}

	@Override
	public synchronized void init() throws Exception {
		if (initialized) {
			throw new IllegalStateException("Already initialized");
		}
		
		logger = LoggerFactory.getLogger(getClass().getSimpleName() + "." + rconfig.getHost());
		
		initialized = true;
		
		try {
			SimpleSshSessionProvider sf = new SimpleSshSessionProvider();
			sf.setUser(rconfig.getAccount());
			if (rconfig.getPassword() != null) {
				sf.setPassword(rconfig.getPassword());
			}
			if (rconfig.getKeyFile() != null) {
				sf.setKeyFile(rconfig.getKeyFile());
			}
			if (rconfig.getAuthMethods() != null) {
				sf.setConfig("PreferredAuthentications", rconfig.getAuthMethods());
			}
			session = sf.getSession(rconfig.getHost(), rconfig.getAccount());
			jarCache = new SftFileCache(session, rconfig.getJarCachePath(), 4);
			initRemoteClasspath();
			startTunneler();
			hub = new RemotingHub(ZLogFactory.getDefaultRootLogger());
			initPortForwarding();
		}
		catch(Exception e) {
			destroyed = true;
			if (session != null) {
				try {
					session.disconnect();
				} catch (Exception ee) {
					// ignore
				}
			}
			throw e;
		}
	}

	private void initRemoteClasspath() throws IOException {
		List<Classpath.ClasspathEntry> classpath = Classpath.getClasspath(Thread.currentThread().getContextClassLoader());

		// random upload order improve performance if cache is on shared mount
		List<Classpath.ClasspathEntry> uploadJars = new ArrayList<Classpath.ClasspathEntry>(classpath);
		Collections.shuffle(uploadJars);
		List<String> rnames = jarCache.upload(uploadJars);
		Map<String, String> pathMap = new HashMap<String, String>();
		for(int i = 0; i != rnames.size(); ++i) {
			pathMap.put(uploadJars.get(i).getUrl().toString(), rnames.get(i));
		}

		StringBuilder remoterClasspath = new StringBuilder();
		for(Classpath.ClasspathEntry ce: classpath) {
			if (remoterClasspath.length() > 0) {
				remoterClasspath.append(' ');
			}
			remoterClasspath.append(pathMap.get(ce.getUrl().toString()));			
		}

		Manifest mf = new Manifest();
		mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mf.getMainAttributes().put(Attributes.Name.CLASS_PATH, remoterClasspath.toString());
		mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Tunneller.class.getName());

		byte[] tunnelerJar = ClasspathUtils.createManifestJar(mf);
		tunnellerJarPath = jarCache.upload(new ByteBlob("tunneller.jar", tunnelerJar));		
	}

	private String createBootJar(String name, JvmConfig config) throws IOException {
		
		List<Classpath.ClasspathEntry> classpath = Classpath.getClasspath(Thread.currentThread().getContextClassLoader());
		classpath = config.filterClasspath(classpath);

		// random upload order improve performance if cache is on shared mount
		List<Classpath.ClasspathEntry> uploadJars = new ArrayList<Classpath.ClasspathEntry>(classpath);
		Collections.shuffle(uploadJars);
		List<String> rnames = jarCache.upload(uploadJars);
		Map<String, String> pathMap = new HashMap<String, String>();
		for(int i = 0; i != rnames.size(); ++i) {
			pathMap.put(uploadJars.get(i).getUrl().toString(), rnames.get(i));
		}

		StringBuilder remoterClasspath = new StringBuilder();
		for(Classpath.ClasspathEntry ce: classpath) {
			if (remoterClasspath.length() > 0) {
				remoterClasspath.append(' ');
			}
			remoterClasspath.append(pathMap.get(ce.getUrl().toString()));			
		}

		Manifest mf = new Manifest();
		mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mf.getMainAttributes().put(Attributes.Name.CLASS_PATH, remoterClasspath.toString());
		mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Bootstraper.class.getName());
		
		byte[] bootJar = ClasspathUtils.createManifestJar(mf);
		String bootJarPath = jarCache.upload(new ByteBlob(makeBootJarName(name), bootJar));

		return bootJarPath;
	}
	
	private String makeBootJarName(String name) {
		// jar is content hashed so nodes with same classpath will receive same name
		// using neutral booter.jar is less confusing
		return "booter.jar";
//		StringBuilder sb = new StringBuilder();
//		for(int i = 0; i != name.length(); ++i) {
//			char ch = name.charAt(i);
//			if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.') {
//				sb.append(ch);				
//			}
//		}
//		if (sb.length() == 0) {
//			return "booter.jar";
//		}
//		else {
//			return sb.append(".jar").toString();
//		}
	}

	private void verifyJavaVersion() throws JSchException, IOException {
		ChannelExec exec = (ChannelExec) session.openChannel("exec");
		
		String cmd = rconfig.getJavaExec() + " -Xms32m -Xmx32m -version";
		exec.setCommand(cmd);
		
		InputStream cin = exec.getInputStream();
		InputStream cerr = exec.getErrStream();
		OutputStream cout = exec.getOutputStream();
		
		PrintStream out = new LoggerPrintStream(createTunnellerOutputLogger(), Level.INFO);

		// unfortunately Pty will merge out and err, so it should be disabled
		exec.setPty(false);
		exec.connect();
		
		cout.close();
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		byte[] buf = new byte[4 << 10];
		while(deadline > System.nanoTime()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			if (cout != null) {
				cout.close();
			}
			if (cin != null) {
				while(true) {
					int n = BackgroundStreamDumper.pullStream(buf, cin, out);
					if (n < 0) {
						cin = null;
						break;
					}
					if (n == 0) {
						break;
					}
				}
			}
			if (cerr != null) {
				while(true) {
					int n = BackgroundStreamDumper.pullStream(buf, cerr, out);
					if (n < 0) {
						cerr = null;
						break;
					}
					if (n == 0) {
						break;
					}
				}
			}
			if (cin == null && cerr == null) {
				// ok
				int excode = exec.getExitStatus();				
				exec.disconnect();
				if (excode != 0) {
					throw new RuntimeException("Failed to execute \"" + cmd + "\", host: " + rconfig.getAccount() + "@" + rconfig.getHost());
				}
				return;
			}
		}
		throw new RuntimeException("Timedout executing \"" + cmd + "\", host: " + rconfig.getAccount() + "@" + rconfig.getHost());
	}
	
	private void startTunneler() throws JSchException, IOException {
		verifyJavaVersion();
		
		ChannelExec exec = (ChannelExec) session.openChannel("exec");
		
		String cmd = rconfig.getJavaExec() + " -Xms32m -Xmx32m -jar " + tunnellerJarPath;
		exec.setCommand(cmd);
		
		// use std out for binary communication
		InputStream cin = exec.getInputStream();
		OutputStream cout = exec.getOutputStream();
		// use std err for diagnostic output
		OutputStream tunnel = new LoggerPrintStream(createTunnellerOutputLogger(), Level.INFO);
		BackgroundStreamDumper.link(exec.getExtInputStream(), tunnel, false);

		// unfortunately Pty will merge out and err, so it should be disabled
		exec.setPty(false);
		exec.connect();

		PrintStream diagLog = new LoggerPrintStream(logger, Level.INFO);
		
		try {
		    long connTimeout = rconfig.getTunnellerTimeout();
		    if (connTimeout == 0) {
		        connTimeout = connectTimeoutMS;
		    }
			control = new TunnellerConnection(rconfig.getHost(), cin, cout, diagLog, connTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			killAndDrop(exec);
			throw new IOException("Connection aborted due to thread interrupt");
		} catch (TimeoutException e) {
			throw new IOException("Tunneller connection timeout");
		}
	}

	private void killAndDrop(ChannelExec exec) {
		try {
			exec.sendSignal("KILL");
		} catch (Exception e) {
			// ignore
		}
		exec.disconnect();
	}

	protected Logger createTunnellerOutputLogger() {
		String loggerName = 
				getClass().getSimpleName()
			+ ".out."
			+ getShortHostName(rconfig.getHost());
		return LoggerFactory.getLogger(loggerName);
	}
	
	private String getShortHostName(String host) {
		int n = host.indexOf('.');
		return n < 0 ? host : host.substring(0, n);
	}

	private void initPortForwarding() throws InterruptedException, ExecutionException, IOException {
		final FutureBox<Void> box = new FutureBox<Void>();
		control.newSocket(new SocketHandler() {
			
			@Override
			public void bound(String host, int port) {
				logger.info("Remote port bound " + host + ":" + port);
				tunnelHost = host;
				tunnelPort = port;
				box.setData(null);				
			}
			
			@Override
			public void accepted(String rhost, int rport, InputStream soIn, OutputStream soOut) {
				logger.info("Inbound connection");
				handleInbound(rhost, rport, soIn, soOut);
			}
		});
		try {
			box.get(15000, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new RuntimeException("Failed to bind remote port due to timeout");
		}
	}
	
	protected void handleInbound(String rhost, int rport, InputStream soIn, OutputStream soOut) {		
		String sname;
		if ("localhost".equals(rhost)) {
			sname = "TUNNEL[" + rconfig.getHost() + "/*:" + rport + "]";
		}
		else {
			sname = "TUNNEL[" + rconfig.getHost() + "/" + rhost + ":" + rport + "]";
		}
		
		DuplexStream ds = new NamedStreamPair(sname, soIn, soOut);
		hub.dispatch(ds);
	}
	
	private synchronized void ensureActive() {
		if (!initialized) {
			throw new IllegalStateException("Not initialized");
		}
		if (destroyed) {
			throw new IllegalStateException("Terminated");
		}
	}
	
	@Override
	public ManagedProcess createProcess(String caption, JvmConfig jvmArgs) throws IOException {
		ensureActive();
		
		String bootJarPath = createBootJar(caption, jvmArgs);
		
		ExecCommand jvmCmd = new ExecCommand(rconfig.getJavaExec());
		jvmArgs.apply(jvmCmd);
		jvmCmd.addArg("-jar")
			.addArg(bootJarPath);
		
		RemoteControlSession session = new RemoteControlSession();
		String sessionId = LegacySpore.uidOf(hub.allocateSession(caption, session));
		jvmCmd.addArg(sessionId).addArg(tunnelHost).addArg(String.valueOf(tunnelPort));
		session.setSessionId(sessionId);

		exec(jvmCmd, session);
		try {
			session.started.get();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted");
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			}
			else {
				throw new IOException(e.getCause());
			}
		}

		return session;
	}
	
	protected void exec(ExecCommand jvmCmd, RemoteControlSession handler) throws IOException {
		String[] vars = getEnviromentArray(jvmCmd); 
		handler.execId = control.exec(jvmCmd.getWorkDir(), jvmCmd.getCommandArray(), vars, handler);		 
	}

	private String[] getEnviromentArray(ExecCommand cmd) {
		Map<String, String> env = cmd.getEviroment();
	    List<String> lines = new ArrayList<String>(env.size());
	    
	    for (Map.Entry<String, String> var : env.entrySet()) {
	        lines.add(var.getKey() + "=" + var.getValue()); 
	    }
	    
	    return lines.toArray(new String[lines.size()]);
	}
	
	@Override
	public synchronized void dispose() {
		if (!destroyed) {
			destroyed = true;
			hub.dropAllSessions();
			session.disconnect();
			
			hub = null;
			session = null;			
		}
	}
	
	private class RemoteControlSession extends ProcessProxy implements SessionEventListener, ManagedProcess, ExecHandler {
		
		long execId;
		String sessionId;
		AdvancedExecutor remoteExecutorService;
		FutureBox<Void> connected = new FutureBox<Void>();
		
		@Override
		public AdvancedExecutor getExecutionService() {
			try {
				connected.get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted");
			} catch (ExecutionException e) {
				throw new RuntimeException("Execution failed", e.getCause());
			}
			return remoteExecutorService;
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}
		
		@Override
		public void connected(DuplexStream stream) {
			remoteExecutorService = hub.getExecutionService(sessionId);
			connected.setData(null);
			logger.info("Conntected: " + stream);
		}

		@Override
		public void interrupted(DuplexStream stream) {
			logger.info("Interrupted: " + stream);
		}

		@Override
		public void reconnected(DuplexStream stream) {
			logger.info("Reconnected: " + stream);
		}

		@Override
		public void suspend() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void resume() {
			throw new UnsupportedOperationException();
		}

		@Override
		public FutureEx<Integer> getExitCodeFuture() {
			// FIXME getExitCodeFuture for remote process
			return new FutureBox<Integer>();
		}

		@Override
		public void bindStdIn(InputStream is) {
			if (is != null) {
				BackgroundStreamDumper.link(is, getOutputStream());
			}
			else {
				try {
					getOutputStream().close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		@Override
		public void bindStdOut(OutputStream os) {
			if (os != null) {
				BackgroundStreamDumper.link(getInputStream(), os);
			}
			else {
				try {
					getInputStream().close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
		}

		@Override
		public void bindStdErr(OutputStream os) {
			if (os != null) {
				BackgroundStreamDumper.link(getErrorStream(), os);
			}
			else {
				try {
					getErrorStream().close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		@Override
		public void closed() {
			kill();
		}

		@Override
		public void destroy() {
			MasterHub hub = TunnellerJvmReplicator.this.hub;
			if (hub != null) {
				hub.dropSession(sessionId);
			}
			kill();
		}

		protected void kill() {
			TunnellerConnection tc = control;
			try {
				if (tc != null) {
					tc.kill(execId);
				}
			} catch (IOException e) {
				// ignore
			}
		}		
	}	
	
	static class ProcessProxy extends Process implements TunnellerConnection.ExecHandler {

		protected FutureBox<Void> started = new FutureBox<Void>();
		protected FutureBox<Integer> exitCode = new FutureBox<Integer>();

		protected OutputStream stdIn;
		protected InputStream stdOut;
		protected InputStream stdErr;
		
		@Override
		public void started(OutputStream stdIn, InputStream stdOut,	 InputStream stdErr) {
			this.stdIn = stdIn;
			this.stdOut = stdOut;
			this.stdErr = stdErr;
			started.setData(null);
		}

		@Override
		public void finished(int exitCode) {
			this.exitCode.setData(exitCode);
		}

		@Override
		public OutputStream getOutputStream() {
			return stdIn;
		}

		@Override
		public InputStream getInputStream() {
			return stdOut;
		}

		@Override
		public InputStream getErrorStream() {
			return stdErr;
		}

		@Override
		public int waitFor() throws InterruptedException {
			try {
				return exitCode.get();
			} catch (ExecutionException e) {
				throw new Error("Impossible");
			}
		}

		@Override
		public int exitValue() {
			if (exitCode.isDone()) {
				try {
					return exitCode.get();
				} catch (InterruptedException e) {
					throw new Error("Impossible");
				} catch (ExecutionException e) {
					throw new Error("Impossible");
				}
			}
			else {
				throw new IllegalThreadStateException();
			}
		}

		@Override
		public void destroy() {
			//  
		}
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
