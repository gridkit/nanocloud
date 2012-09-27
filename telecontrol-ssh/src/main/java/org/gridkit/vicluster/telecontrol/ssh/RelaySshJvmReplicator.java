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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.ExecCommand;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.JvmProcessFactory;
import org.gridkit.vicluster.telecontrol.bootstraper.Bootstraper;
import org.gridkit.vicluster.telecontrol.bootstraper.HalloWorld;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.RmiGateway;
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
public class RelaySshJvmReplicator implements JvmProcessFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(RelaySshJvmReplicator.class);
	
	private SshSessionFactory factory;
	private String host;
	private String account;
	private String agentHome; 
	private String javaExecPath = "java";
	
	private List<SessionInfo> sessionCache = new ArrayList<SessionInfo>();
	private RemoteFileCache remoteCache;
	private Random random = new Random();
	
	private String bootJarPath;
	
	private RemotingHub hub = new RemotingHub();
	private int controlPort;

	public RelaySshJvmReplicator(String host, String account, SshSessionFactory sshFactory) {
		this.host = host;
		this.account = account;
		this.factory = sshFactory;
	}

	public RelaySshJvmReplicator(Session session) {
		initPortForwarding(session);
		sessionCache.add(new SessionInfo(session));
	}
	
	public void setAgentHome(String agentHome) {
		this.agentHome = agentHome;
	}

	public void setJavaExecPath(String javaExecPath) {
		this.javaExecPath = javaExecPath;
	}

	public void init() throws JSchException, SftpException, IOException, InterruptedException {
		remoteCache = new RemoteFileCache();
		remoteCache.setAgentHome(agentHome);
		remoteCache.setSession(getSession());
		remoteCache.init();

		initRemoteClassPath();
		
		ExecCommand halloWorldCmd = new ExecCommand(javaExecPath);
		halloWorldCmd.setWorkDir(agentHome);
		halloWorldCmd.addArg("-cp").addArg(bootJarPath).addArg(HalloWorld.class.getName());
		RemoteSshProcess rp = new RemoteSshProcess(getSession(), halloWorldCmd);
		rp.getOutputStream().close();
		BackgroundStreamDumper.link(rp.getInputStream(), System.out);
		BackgroundStreamDumper.link(rp.getErrorStream(), System.err);
		int exitCode = rp.waitFor();
		if (exitCode != HalloWorld.EXIT_CODE) {
			throw new IOException("Failed to start remote JVM, exit code: " + exitCode);
		}
	}
	
	@Override
	private ControlledProcess createDirectProcess(String caption, JvmConfig jvmArgs) throws IOException {
		ExecCommand jvmCmd = new ExecCommand(javaExecPath);
		jvmCmd.setWorkDir(agentHome);
		jvmArgs.apply(jvmCmd);
		jvmCmd.addArg("-jar").addArg(bootJarPath);
		
		RemoteControlSession session = new RemoteControlSession();
		String sessionId = hub.newSession(caption, session);
		jvmCmd.addArg(sessionId).addArg("127.0.0.1").addArg(String.valueOf(controlPort));
		jvmCmd.addArg(agentHome);
		session.setSessionId(sessionId);
		
		RemoteSshProcess rp;
		try {
			rp = new RemoteSshProcess(getSession(), jvmCmd);
		} catch (JSchException e) {
			throw new IOException(e);
		}
		session.setProcess(rp);
		
		return session;
	}
	
	@Override
	public ControlledProcess createProcess(String caption, JvmConfig jvmArgs) throws IOException {
		return null;
	}

	private synchronized Session getSession() throws JSchException {
		if (factory == null) {
			return sessionCache.get(0).session;
		}
		else {
			if (sessionCache.isEmpty()) {
				pushNewSession();
				return sessionCache.get(0).session;
			}
			else {
				SessionInfo ssh = sessionCache.get(0);
				// TODO is 5 ok?
				if (ssh.usage < 3) {
					return ssh.session;
				}
				else {
					pushNewSession();
					return sessionCache.get(0).session;
				}
			}
		}
	}	

	private void pushNewSession() throws JSchException {
		Session ssh = factory.getSession(host, account);
		initPortForwarding(ssh);
		SessionInfo si = new SessionInfo(ssh);
		si.usage = 1;
		sessionCache.add(0, si);
	}

	public synchronized void shutdown() {
		for(SessionInfo si: sessionCache) {
			si.session.disconnect();
		}
	}

	private synchronized void initPortForwarding(Session ssh) {
		for(int i = 0; i != 10; ++i) {
			int port;
			try {
				port = 50000 + random.nextInt(1000);
				ssh.setPortForwardingR(port, RelaySshJvmReplicator.class.getName() + "$" + RemotingTunnelAcceptor.class.getSimpleName(), new Object[]{RelaySshJvmReplicator.this});
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
		List<URL> cpURLs = new ArrayList<URL>(ClasspathUtils.listCurrentClasspath());

		Map<String, String> pathMap = new HashMap<String, String>();
		// random upload order improve performance if cache is on shared mount
		List<URL> uploadURLs = new ArrayList<URL>(cpURLs);
		Collections.shuffle(uploadURLs);
		for(URL url: uploadURLs) {
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
			pathMap.put(url.toString(), remoteCache.upload(lname, data));
		}

		StringBuilder remoterClasspath = new StringBuilder();
		for(URL url: cpURLs) {
			if (remoterClasspath.length() > 0) {
				remoterClasspath.append(' ');
			}
			remoterClasspath.append(pathMap.get(url.toString()));			
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
		RmiGateway gateway;
		RemoteSshProcess process;
		CountDownLatch connected = new CountDownLatch(1);
		
		@Override
		public Process getProcess() {
			return process;
		}

		@Override
		public AdvancedExecutor getExecutionService() {
			return getRemoteExecutor();
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public void setProcess(RemoteSshProcess process) {
			this.process = process;
		}

		public AdvancedExecutor getRemoteExecutor() {
			try {
				connected.await();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return gateway == null ? null : gateway.asExecutor();
		}
		
		@Override
		public void connected(DuplexStream stream) {
			gateway = hub.getGateway(sessionId);
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

		@Override
		public void destroy() {
			hub.closeConnection(sessionId);
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
		private RelaySshJvmReplicator host;
		
		public RemotingTunnelAcceptor() {
		}
		
		@Override
		public void setArg(Object[] arg) {
			host = (RelaySshJvmReplicator) arg[0];
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
	
	private static class SessionInfo {
		Session session;
		int usage;
		
		public SessionInfo(Session session) {
			this.session = session;
		}
	}
}
