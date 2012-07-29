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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.ExecCommand;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.JvmProcessFactory;
import org.gridkit.vicluster.telecontrol.bootstraper.Bootstraper;
import org.gridkit.vicluster.telecontrol.bootstraper.HalloWorld;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.hub.RemotingHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelForwardedTCPIP;
import com.jcraft.jsch.ForwardedTCPIPDaemon;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SshJvmReplicator implements JvmProcessFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SshJvmReplicator.class);
	
	private SshSessionProvider factory;
	private String host;
	private String agentHome; 
	private String javaExecPath = "java";
	
	private Session ssh;
	private RemoteFileCache remoteCache;
	private Random random = new Random();
	
	private String bootJarPath;
	
	private RemotingHub hub = new RemotingHub();
	private int controlPort;

	public SshJvmReplicator(String host, SshSessionProvider sshFactory) {
		this.host = host;
		this.factory = sshFactory;
	}
	
	public void setAgentHome(String agentHome) {
		this.agentHome = agentHome;
	}

	public void setJavaExecPath(String javaExecPath) {
		this.javaExecPath = javaExecPath;
	}

	public void init() throws JSchException, SftpException, IOException, InterruptedException {
		ssh = factory.getSession(host, null);
		remoteCache = new RemoteFileCache();
		remoteCache.setAgentHome(agentHome);
		remoteCache.setSession(ssh);
		remoteCache.init();

		initRemoteClassPath();
		
		initPortForwarding();
		
		ExecCommand halloWorldCmd = new ExecCommand(javaExecPath);
		halloWorldCmd.setWorkDir(agentHome);
		halloWorldCmd.addArg("-cp").addArg(bootJarPath).addArg(HalloWorld.class.getName());
		RemoteSshProcess rp = new RemoteSshProcess(ssh, halloWorldCmd);
		rp.getOutputStream().close();
		BackgroundStreamDumper.link(rp.getInputStream(), System.out);
		BackgroundStreamDumper.link(rp.getErrorStream(), System.err);
		
		rp.waitFor();
		Thread.sleep(2000);
	}
	
	@Override
	public ControlledProcess createProcess(JvmConfig jvmArgs) throws IOException {
		ExecCommand jvmCmd = new ExecCommand(javaExecPath);
		jvmCmd.setWorkDir(agentHome);
		jvmCmd.addArg("-jar").addArg(bootJarPath);
		
		RemoteControlSession session = new RemoteControlSession();
		String sessionId = hub.newSession(session);
		jvmCmd.addArg(sessionId).addArg("localhost").addArg(String.valueOf(controlPort));
		jvmCmd.addArg(agentHome);
		session.setSessionId(sessionId);
		
		RemoteSshProcess rp;
		try {
			rp = new RemoteSshProcess(ssh, jvmCmd);
		} catch (JSchException e) {
			throw new IOException(e);
		}
		session.setProcess(rp);
		
		return session;
	}

	public ExecutorService createRemoteExecutor() throws JSchException, IOException {
		ExecCommand jvmCmd = new ExecCommand(javaExecPath);
		jvmCmd.setWorkDir(agentHome);
		jvmCmd.addArg("-jar").addArg(bootJarPath);
		
		RemoteControlSession session = new RemoteControlSession();
		String sessionId = hub.newSession(session);
		jvmCmd.addArg(sessionId).addArg("localhost").addArg(String.valueOf(controlPort));
		jvmCmd.addArg(agentHome);
		session.setSessionId(sessionId);
		
		RemoteSshProcess rp = new RemoteSshProcess(ssh, jvmCmd);
		rp.getOutputStream().close();
		BackgroundStreamDumper.link(rp.getInputStream(), System.out);
		BackgroundStreamDumper.link(rp.getErrorStream(), System.err);
		session.setProcess(rp);
		
		return session.getRemoteExecutor();
	}

	private synchronized void initPortForwarding() {
		for(int i = 0; i != 10; ++i) {
			int port;
			try {
				port = 50000 + random.nextInt(1000);
				ssh.setPortForwardingR(port, SshJvmReplicator.class.getName() + "$" + RemotingTunnelAcceptor.class.getSimpleName(), new Object[]{SshJvmReplicator.this});
			}
			catch(JSchException e) {
				LOGGER.warn("Failed to forward port " + e.toString());
				continue;
			}
			controlPort = port;
			return;
		}
		throw new RuntimeException("Failed to bind remote port");
	}

	private void initRemoteClassPath() throws IOException, SftpException {
		StringBuilder remoterClasspath = new StringBuilder();
		for(URL url: ClasspathUtils.listCurrentClasspath()) {
			byte[] data;
			String lname;
			try {
				File file = new File(url.toURI());
				if (file.isFile()) {
					data = readFile(file);
					lname = file.getName();
				}
				else {
					lname = file.getName();
					if ("classes".equals(lname)) {
						lname = file.getParentFile().getName();
					}
					if ("target".equals(lname)) {
						lname = file.getParentFile().getParentFile().getName();
					}
					lname += ".jar";
					data = ClasspathUtils.jarFiles(file.getPath());
				}
			}
			catch(Exception e) {
				LOGGER.warn("Cannot copy to remote host URL " + url.toString(), e);
				continue;
			}
			String name = remoteCache.upload(lname, data);
			if (remoterClasspath.length() > 0) {
				remoterClasspath.append(' ');
			}
			remoterClasspath.append(name);
		}
		Manifest mf = new Manifest();
		mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mf.getMainAttributes().put(Attributes.Name.CLASS_PATH, remoterClasspath.toString());
		mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Bootstraper.class.getName());
		
		byte[] bootJar = ClasspathUtils.createManifestJar(mf);
		bootJarPath = remoteCache.upload("booter.jar", bootJar);
	}
	
	private byte[] readFile(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		StreamHelper.copy(fis, bos);
		bos.close();
		return bos.toByteArray();
	}
	
	private class RemoteControlSession implements SessionEventListener, ControlledProcess {
		
		String sessionId;
		ExecutorService remoteExecutorService;
		RemoteSshProcess process;
		CountDownLatch connected = new CountDownLatch(1);
		
		@Override
		public Process getProcess() {
			return process;
		}

		@Override
		public ExecutorService getExecutionService() {
			return getRemoteExecutor();
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public void setProcess(RemoteSshProcess process) {
			this.process = process;
		}

		public ExecutorService getRemoteExecutor() {
			try {
				connected.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return remoteExecutorService;
		}
		
		@Override
		public void connected(DuplexStream stream) {
			remoteExecutorService = hub.getExecutionService(sessionId);
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
	
	private static class TunneledSocket implements DuplexStream {
		
		private ChannelForwardedTCPIP channel;
		private InputStream in;
		private OutputStream out;

		public TunneledSocket(ChannelForwardedTCPIP channel, InputStream in, OutputStream out) {
			this.channel = channel;
			this.in = in;
			this.out = out;
		}

		@Override
		public InputStream getInput() throws IOException {
			return in;
		}

		@Override
		public OutputStream getOutput() throws IOException {
			return out;
		}

		@Override
		public boolean isClosed() {
			return channel.isConnected();
		}

		@Override
		public void close() throws IOException {
			channel.disconnect();
		}

		@Override
		public String toString() {
			try {
				return "[SSH Tunnel: " + channel.getSession().getHost() + ":" + channel.getRemotePort() + "]";
			} catch (JSchException e) {
				return "[SSH Tunnel: ?:" + channel.getRemotePort() + "]";
			}
		}
	}
	
	public static class RemotingTunnelAcceptor implements ForwardedTCPIPDaemon {

		private ChannelForwardedTCPIP channel;
		private InputStream in;
		private OutputStream out;
		private SshJvmReplicator host;
		
		public RemotingTunnelAcceptor() {
		}
		
		@Override
		public void setArg(Object[] arg) {
			host = (SshJvmReplicator) arg[0];
		}

		@Override
		public void setChannel(ChannelForwardedTCPIP channel, InputStream in, OutputStream out) {
			this.channel = channel;
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {
			try {
				LOGGER.debug("SSH Tunnel: incomming " + channel.getSession().getHost() + ":" + channel.getId());
				TunneledSocket ts = new TunneledSocket(channel, in, out);
				host.hub.dispatch(ts);
			} catch (JSchException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
