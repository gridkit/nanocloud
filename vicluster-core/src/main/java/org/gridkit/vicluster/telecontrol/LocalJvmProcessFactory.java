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
package org.gridkit.vicluster.telecontrol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.telecontrol.bootstraper.Bootstraper;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.SocketStream;
import org.gridkit.zerormi.hub.LegacySpore;
import org.gridkit.zerormi.hub.RemotingHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class LocalJvmProcessFactory implements JvmProcessFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalJvmProcessFactory.class);
	
	private String javaHome;
	private String defaultClasspath;

	// TODO configure ZLog
	private RemotingHub hub = new RemotingHub(ZLogFactory.getDefaultRootLogger());
	private ServerSocket socket;
	private Thread accepter;
	private List<Process> processes = new ArrayList<Process>();
	
	public LocalJvmProcessFactory() {
		javaHome = System.getProperty("java.home");
		defaultClasspath = sanitize(System.getProperty("java.class.path"));		
		
		initHubSocket();
	}

	private String sanitize(String cp) {
		StringBuilder sb = new StringBuilder();
		String psep = System.getProperty("path.separator");
		for(String path: cp.split(psep)) {
			try {
				String spath = new File(path).getAbsoluteFile().getCanonicalPath();
				if (sb.length() > 0) {
					sb.append(psep);
				}
				sb.append(spath);
			}
			catch(IOException e) {
				// ignore erroneous classpath element
			}
		}
		return sb.toString();
	}

	private void initHubSocket() {
		try {
			
			SocketAddress addr = new InetSocketAddress("127.0.0.1", 0);
			ServerSocket socket = new ServerSocket();
			socket.bind(addr);
			this.socket = socket;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Runnable accepterTask = new Runnable() {
			@Override
			public void run() {
				try {					
					while(!socket.isClosed()) {
						Socket conn = socket.accept();
						hub.dispatch(new SocketStream(conn));
					}
				}
				catch(IOException e) {
					LOGGER.warn("ACCEPTER: " + e.toString());
				}
			}
		};
		
		accepter = new Thread(accepterTask);
		accepter.setDaemon(true);
		accepter.setName("Control hub accepter [127.0.0.1:" + socket.getLocalPort() + "]");
		accepter.start();
	}

	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}

	public void setDefaultClasspath(String defaultClasspath) {
		this.defaultClasspath = defaultClasspath;
	}

	public void stop() {
		accepter.interrupt();
		try {
			socket.close();
		} catch (IOException e) {
			// ignore
		}
		for(Process p: processes) {
			try {
				p.destroy();
			}
			catch(Exception e) {
				LOGGER.info("Process termination failed. " + e.toString());
			}
		}
	}
	
	@Override
	public ManagedProcess createProcess(String caption, JvmConfig jvmArgs) throws IOException {

		RemoteControlSession session;

		String filesep = System.getProperty("file.separator");
		ExecCommand jvmCmd = new ExecCommand(javaHome + filesep + "bin" + filesep + "java");
		jvmCmd.addArg("-cp").addArg(jvmArgs.filterClasspath(defaultClasspath));
		jvmArgs.apply(jvmCmd);
		jvmCmd.addArg(Bootstraper.class.getName());

		synchronized(this) {
			
			session = new RemoteControlSession();
			String sessionId = LegacySpore.uidOf(hub.allocateSession(caption, session));
			jvmCmd.addArg(sessionId).addArg("localhost").addArg(String.valueOf(socket.getLocalPort()));
			session.setSessionId(sessionId);
			
		}
		
		Process p = startProcess(caption, jvmCmd);
		
		synchronized(this) {
			enlist(p);
			session.setProcess(p);
		}
		
		while(true) {
			AdvancedExecutor exec = session.ensureRemoteExecutor(100);
			if (exec != null) {
				break;
			}
			try {
				int code = p.exitValue();
				StreamHelper.copy(p.getInputStream(), System.out);
				StreamHelper.copy(p.getErrorStream(), System.err);
				p.destroy();
				unlist(p);
				throw new IOException("Child JVM process has terminated, exit code " + code);
			}
			catch(IllegalThreadStateException e) {
				// process is still alive
			}
		}
		
		return session;
	}

	protected Process startProcess(String name, ExecCommand jvmCmd) throws IOException {
		ProcessBuilder pb;
		pb = jvmCmd.getProcessBuilder();
		Process p;
		p = pb.start();
		return p;
	}
	
	private synchronized void enlist(Process p) {
		processes.add(p);	
	}

	private void unlist(Process p) {
		processes.remove(p);
	}

	private class RemoteControlSession implements SessionEventListener, ManagedProcess {
		
		String sessionId;
		Process process;
		AdvancedExecutor executor;
		CountDownLatch connected = new CountDownLatch(1);
		
		@Override
		public AdvancedExecutor getExecutionService() {
			return ensureRemoteExecutor(-1);
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public void setProcess(Process process) {
			this.process = process;
		}

		@SuppressWarnings("unused")
		private boolean isConnected() {
			return connected.getCount() == 0;
		}
		
		private AdvancedExecutor ensureRemoteExecutor(long timeout) {
			try {
				if (timeout < 0) {
					connected.await();
				}
				else {
					connected.await(timeout, TimeUnit.MILLISECONDS);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return executor;
		}
		
		@Override
		public void connected(DuplexStream stream) {
			executor = hub.getExecutionService(sessionId);
			connected.countDown();
			LOGGER.info("Conntected: " + stream);
		}

		@Override
		public void interrupted(DuplexStream stream) {
			LOGGER.info("Interrupted: " + stream);
		}

		@Override
		public void reconnected(DuplexStream stream) {
			LOGGER.info("Reconnected: " + stream);
		}

		@Override
		public void bindStdIn(InputStream is) {
			if (is != null) {
				BackgroundStreamDumper.link(is, process.getOutputStream());
			}
			else {
				try {
					process.getOutputStream().close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public void bindStdOut(OutputStream os) {
			if (os != null) {
				BackgroundStreamDumper.link(process.getInputStream(), os);
			}
			else {
				try {
					process.getInputStream().close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
		}

		@Override
		public void bindStdErr(OutputStream os) {
			if (os != null) {
				BackgroundStreamDumper.link(process.getErrorStream(), os);
			}
			else {
				try {
					process.getErrorStream().close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
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
		public void consoleFlush() {
			// do nothing
		}

		@Override
		public void destroy() {
			closed();
		}

		@Override
		public FutureEx<Integer> getExitCodeFuture() {
			// FIXME getExitCodeFuture() not implemented
			FutureBox<Integer> fb = new FutureBox<Integer>();
			return fb;
		}

		@Override
		public void closed() {
			LOGGER.info("Closed");
			process.destroy();
			unlist(process);
		}
	}	
}
