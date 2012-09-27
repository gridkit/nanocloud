package org.gridkit.vicluster.telecontrol.spi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.spi.Host;
import org.gridkit.vicluster.spi.JvmProcessConfiguration;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshJvmReplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SshControlledHost implements Host {

	private static final Logger LOGGER = LoggerFactory.getLogger(SshControlledHost.class);
	
	private final String host;
	private final SshConnector connector;
	private String javaExec = "java";
	private String agentTempDir = ".gridagent";		

	private Session sshSession; 
	private SimpleSshJvmReplicator session;
	private List<ControlledProcess> processes = new ArrayList<ControlledProcess>();
	
	public SshControlledHost(String host, SshConnector connector) {
		this.host = host;
		this.connector = connector;
	}
	
	@Override
	public String getHostname() {
		return host;
	}

	private synchronized void ensureConnected() throws IOException, JSchException, SftpException {
		if (sshSession == null || !sshSession.isConnected()) {
			sshSession = null;
			session = null;
			dropProcesses();
			
			int tries = 2;
			while(true) {
				try {
					connect();
					return;
				}
				catch(Exception e) {
					if (e instanceof IOException || e instanceof JSchException || e instanceof SftpException) {
						if (--tries <= 0) {
							Any.throwUncheked(e);
						}
						Any.throwUncheked(e);
					}
				}
			}
		}
	}
	
	private void connect() throws IOException, JSchException, SftpException, InterruptedException {
		sshSession = connector.connect();
		session = new SimpleSshJvmReplicator(sshSession);
		session.setJavaExecPath(javaExec);
		session.setAgentHome(agentTempDir);
		session.init();		
	}

	private synchronized void disconnect() {
		try {
			session.shutdown();
			sshSession.disconnect();
		}
		catch(Exception e) {
			// ignore
		}
		LOGGER.info("SSH session disconnected " + connector);
		sshSession = null;
		session = null;
	}
	
	private void dropProcesses() {
		for(ControlledProcess proc: processes) {
			try {
				proc.destroy();
			}
			catch(Exception e) {
				// ignore;
			}
		}
		processes.clear();
	}

	@Override
	public synchronized boolean verify() {
		try {
			ensureConnected();
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}

	public void setAgentHome(String agentHome) {
		agentTempDir = agentHome;
	}

	public void setDefaultJava(String defaultJava) {
		javaExec = defaultJava;		
	}	

	@Override
	public synchronized ControlledProcess startProcess(JvmProcessConfiguration configuration) throws IOException {
		try {
			ensureConnected();
			
			String name = configuration.getName();
			JvmConfig jconfig = new JvmConfig();
			for(String option: configuration.getJvmOptions()) {
				jconfig.addOption(option);
			}
			
			ControlledProcess proc = session.createProcess(name, jconfig);
			RemoteProcess rproc = new RemoteProcess(proc);
			
			processes.add(rproc);
			
			return rproc;
		} catch (JSchException e) {
			throw new IOException(e);
		} catch (SftpException e) {
			throw new IOException(e);
		}
	}
	
	private synchronized void removeProcess(RemoteProcess rproc) {
		processes.remove(rproc);
		if (processes.isEmpty()) {
			disconnect();
		}
	}
	
	private class RemoteProcess implements ControlledProcess {
	
		private final ControlledProcess sshProcess;

		public RemoteProcess(ControlledProcess sshProcess) {
			this.sshProcess = sshProcess;
		}

		@Override
		public Process getProcess() {
			return sshProcess.getProcess();
		}

		@Override
		public AdvancedExecutor getExecutionService() {
			return sshProcess.getExecutionService();
		}

		@Override
		public void destroy() {
			try {
				sshProcess.destroy();
			}
			finally{
				removeProcess(this);
			}
		}
	}
}
