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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.SftpException;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshJvmReplicator;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.junit.Test;


public class RemoteJvmCheck {

	@Test
	public void check_ssh_execution() throws JSchException, SftpException, IOException, InterruptedException {
		
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("ubuntu");
		sshFactory.setPassword("reverse");
		SimpleSshJvmReplicator host = new SimpleSshJvmReplicator("localhost:11022", null, sshFactory);
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
