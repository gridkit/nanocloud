package org.gridkit.vicluster.telecontol.ssh;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.gridkit.vicluster.telecontrol.ssh.SshJvmReplicator;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class RemoteJvmCheck {

	@Test
	public void check_ssh_execution() throws JSchException, SftpException, IOException, InterruptedException {
		
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("ubuntu");
		sshFactory.setPassword("reverse");
		SshJvmReplicator host = new SshJvmReplicator("localhost:11022", sshFactory);
		host.setAgentHome(".gridlab-agent");
		host.init();
		
		JvmNodeProvider nodeProvider = new JvmNodeProvider(host);
		
		ViNode node = nodeProvider.createNode("ssh-node", new ViNodeConfig());
		
		node.exec(new VoidCallable() {
			@Override
			public void call() throws UnknownHostException {
				System.out.println("Hallo from " + InetAddress.getLocalHost().getHostName());
			}
		});
		
		node.shutdown();
	}
	
}
