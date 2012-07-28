package org.gridkit.gatling.remoting;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.gridkit.fabric.ssh.DefaultSSHFactory;
import org.gridkit.util.vicontrol.ViNode;
import org.gridkit.util.vicontrol.ViNodeConfig;
import org.gridkit.util.vicontrol.VoidCallable;
import org.gridkit.util.vicontrol.jvm.JvmNodeProvider;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class RemoteJvmCheck {

	@Test
	public void check_ssh_execution() throws JSchException, SftpException, IOException, InterruptedException {
		
		DefaultSSHFactory sshFactory = new DefaultSSHFactory();
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
