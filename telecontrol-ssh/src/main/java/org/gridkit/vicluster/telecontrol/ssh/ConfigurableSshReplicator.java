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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.vicluster.HostSideHook;
import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ConfigurableSshReplicator implements ViNodeProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableSshReplicator.class);
	
	private SshSessionFactory sshFactory;
	private Map<String, SessionInfo> sessions = new HashMap<String, SessionInfo>();
	private ViNodeConfig defaultConfig = new ViNodeConfig();
	
	public ConfigurableSshReplicator(SshSessionFactory sshFactory) {
		this.sshFactory = sshFactory;
		RemoteNodeProps.setRemoteAccount(defaultConfig, "");
		RemoteNodeProps.setRemoteJarCachePath(defaultConfig, ".gridagent");
	}

	public ViConfigurable getDefaultConfig() {
		return defaultConfig;
	}
	
	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		// TODO config verification
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		
		ViNodeConfig effectiveConfig = new ViNodeConfig();
		defaultConfig.apply(effectiveConfig);
		config.apply(effectiveConfig);

		SessionInfo session;

		synchronized(this) {
			
			String host = effectiveConfig.getProp(RemoteNodeProps.HOST);
			if (host == null) {
				throw new IllegalArgumentException("Remote host is not specified");
			}
			String account = effectiveConfig.getProp(RemoteNodeProps.ACCOUNT);
			String javaExec = effectiveConfig.getProp(RemoteNodeProps.JAVA_EXEC);
			String jarCache = effectiveConfig.getProp(RemoteNodeProps.JAR_CACHE_PATH);
	
			String key = host + "|" + account + "|" + javaExec + "|" + jarCache;
			
			session = sessions.get(key);
			
			if (session == null) {
				session = new SessionInfo();
				session.host = host;
				session.account = account.length() == 0 ? null : account;
				session.javaExec = javaExec;
				session.jarCachePath = jarCache;
				sessions.put(key, session);
			}
		}
		
		synchronized (session) {
			if (session.session == null) {
				session.session = new SimpleSshJvmReplicator(session.host, session.account, sshFactory);
				try {
					session.session.setJavaExecPath(session.javaExec);
					session.session.setAgentHome(session.jarCachePath);
					session.session.init();
				} catch (Exception e) {
					session.session = null;
					throw new RuntimeException("SSH connection failed: " + session.host, e);
				}
			}
			
			final SessionInfo context = session;
			final ViNode node = new JvmNodeProvider(session.session).createNode(name, effectiveConfig);			
			node.addShutdownHook("release-ssh", new HostSideHook() {
				
				@Override
				public void run() {
					throw new UnsupportedOperationException();
				}
				
				@Override
				public void hostRun(boolean shutdown) {
					if (shutdown) {
						releaseConnection(context, node);
					}
				}
			}, false);
			session.processes.add(node);
			
			return node;			
		}		
	}
	
	private void releaseConnection(SessionInfo session, ViNode connection) {
		synchronized(session) {
			session.processes.remove(connection);
			if (session.processes.isEmpty()) {
				LOGGER.info("Session " + session + " is not used");
				session.session.shutdown();
				session.session = null;
			}
		}
	}

	private static class SessionInfo {
		String host;
		String account;
		String javaExec;
		String jarCachePath;		
		SimpleSshJvmReplicator session;
		List<ViNode> processes = new ArrayList<ViNode>();
	}
}
