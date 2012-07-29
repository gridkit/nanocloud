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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.gridkit.vicluster.telecontrol.bootstraper.Bootstraper;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.SocketStream;
import org.gridkit.zerormi.hub.RemotingHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalJvmProcessFactory implements JvmProcessFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalJvmProcessFactory.class);
	
	private String javaHome;
	private String defaultClasspath;
	
	private RemotingHub hub = new RemotingHub();
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
		Random rnd = new Random();
		int n = 50;
		while(n > 0) {
			--n;
			int port = 40000 + rnd.nextInt(10000);
			try {
				
				SocketAddress addr = new InetSocketAddress("127.0.0.1", port);
				ServerSocket socket = new ServerSocket();
				socket.bind(addr);
				this.socket = socket;
			} catch (IOException e) {
				LOGGER.info("Failed to bind 127.0.0.1:" + port + ", " + e.toString());
			}
		}
		if (socket == null) {
			throw new RuntimeException("Failed to bind socket for slave communications");
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
	public synchronized ControlledProcess createProcess(JvmConfig jvmArgs) throws IOException {

		String filesep = System.getProperty("file.separator");
		ExecCommand jvmCmd = new ExecCommand(javaHome + filesep + "bin" + filesep + "java");
		jvmCmd.addArg("-cp").addArg(defaultClasspath);
		jvmArgs.apply(jvmCmd);
		jvmCmd.addArg(Bootstraper.class.getName());
		
		RemoteControlSession session = new RemoteControlSession();
		String sessionId = hub.newSession(session);
		jvmCmd.addArg(sessionId).addArg("localhost").addArg(String.valueOf(socket.getLocalPort()));
		session.setSessionId(sessionId);
		
		ProcessBuilder pb = jvmCmd.getProcessBuilder();		
		
		Process p;

		p = pb.start();
		processes.add(p);

//		p.getOutputStream().close();
//		BackgroundStreamDumper.link(p.getInputStream(), System.out);
//		BackgroundStreamDumper.link(p.getErrorStream(), System.err);
		session.setProcess(p);
		
		while(true) {
			ExecutorService exec = session.ensureRemoteExecutor(100);
			if (exec != null) {
				break;
			}
			try {
				int code = p.exitValue();
				StreamHelper.copy(p.getInputStream(), System.out);
				StreamHelper.copy(p.getErrorStream(), System.err);
				p.destroy();
				throw new IOException("Child JVM process has terminated, exit code " + code);
			}
			catch(IllegalThreadStateException e) {
				// process is still alive
			}
		}
		
		return session;
	}
	
	private class RemoteControlSession implements SessionEventListener, ControlledProcess {
		
		String sessionId;
		Process process;
		ExecutorService executor;
		CountDownLatch connected = new CountDownLatch(1);
		
		@Override
		public Process getProcess() {
			return process;
		}
		
		@Override
		public ExecutorService getExecutionService() {
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
		
		private ExecutorService ensureRemoteExecutor(long timeout) {
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
		public void closed() {
			LOGGER.info("Closed");
			process.destroy();
		}
	}	
}
