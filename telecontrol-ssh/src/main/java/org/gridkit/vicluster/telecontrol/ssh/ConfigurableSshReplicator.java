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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.vicluster.HostSideHook;
import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.WildProps;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ConfigurableSshReplicator implements ViNodeProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableSshReplicator.class);
	
	private Map<String, WildProps> sshConfCache = new HashMap<String, WildProps>();
	private Map<String, SessionInfo> sessions = new HashMap<String, SessionInfo>();
	private ViNodeConfig defaultConfig = new ViNodeConfig();
	
	public ConfigurableSshReplicator() {
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
			
			SshSessionConfig sc = resolveSsh(name, effectiveConfig);
			
			if (sc.host == null) {
				throw new IllegalArgumentException("Remote host is not specified for node '" + name + "'");
			}

			String key = sc.toString();
			
			session = sessions.get(key);
			
			if (session == null) {
				session = new SessionInfo();
				session.config = sc;
				sessions.put(key, session);
			}
		}
		
		synchronized (session) {
			if (session.session == null) {
				SimpleSshSessionProvider ssh = new SimpleSshSessionProvider();
				ssh.setUser(session.config.account);
				if (session.config.password != null) {
					ssh.setPassword(session.config.password);
				}
				if (session.config.keyFile != null) {
					ssh.setKeyFile(session.config.keyFile);
				}
				
				session.session = new SimpleSshJvmReplicator(session.config.host, session.config.account, ssh);
				try {
					session.session.setJavaExecPath(session.config.javaExec);
					session.session.setAgentHome(session.config.jarCachePath);
					session.session.init();
				} catch (Exception e) {
					session.session = null;
					throw new RuntimeException("SSH connection failed: " + session.config.host, e);
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

	private synchronized SshSessionConfig resolveSsh(String name, ViNodeConfig nodeConfig) {
		SshSessionConfig s = new SshSessionConfig();
		s.host = nodeConfig.getProp(RemoteNodeProps.HOST);
		if (s.host == null) {
			throw new IllegalArgumentException("No host defined for '" + name + "'");
		}
		if (s.host.startsWith("~")) {
			s.host = transform(s.host, name);
		}
		WildProps sshconf = getConf(nodeConfig.getProp(RemoteNodeProps.SSH_CREDENTIAL_FILE));
		if (sshconf != null) {
			s.account = sshconf.get(s.host);
			s.account = override(s.account, nodeConfig.getProp(RemoteNodeProps.ACCOUNT));
			s.password = sshconf.get(s.account + "@" + s.host + "!password");
			s.keyFile = sshconf.get(s.account + "@" + s.host + "!private-key");
		}
		
		s.account = override(s.account, nodeConfig.getProp(RemoteNodeProps.ACCOUNT));
		s.password = override(s.password, nodeConfig.getProp(RemoteNodeProps.PASSWORD));
		s.keyFile = override(s.keyFile, nodeConfig.getProp(RemoteNodeProps.SSH_KEY_FILE));
		s.javaExec = override(s.javaExec, nodeConfig.getProp(RemoteNodeProps.JAVA_EXEC));
		s.jarCachePath = override(s.jarCachePath, nodeConfig.getProp(RemoteNodeProps.JAR_CACHE_PATH));
		
		if (s.account == null) {
			throw new IllegalArgumentException("No account found for node '" + name + "'");
		}
		
		if (s.password == null && s.keyFile == null) {
			throw new IllegalArgumentException("No creadetials found for node '" + name + "'");
		}
		
		return s;
	}
	
	private String override(String def, String override) {
		if (override != null) {
			return override;
		}
		else {
			return def;
		}
	}

	private synchronized WildProps getConf(String path) {
		if (path == null) {
			return null;
		}
		else {
			if (sshConfCache.containsKey(path)) {
				return sshConfCache.get(path);
			}
			else {
				try {
					InputStream is = null;
					if (path.startsWith("~/")) {
						String userHome = System.getProperty("user.home");
						File cpath = new File(new File(userHome), path.substring(2));
						is = new FileInputStream(cpath);
					}
					else if (path.startsWith("resource:")) {
						String rpath = path.substring("resource:".length());
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
						is = cl.getResourceAsStream(rpath);
						if (is == null) {
							throw new FileNotFoundException("Resource not found '" + path + "'");
						}
					}
					else {
						if (new File(path).exists()) {
							is = new FileInputStream(new File(path));
						}
						else {
							try {
								is = new URL(path).openStream();
							}
							catch(IOException e) {
								// ignore
							}
							if (is == null) {
								throw new FileNotFoundException("Cannot resolve path '" + path + "'");
							}
						}
					}
					WildProps wp = new WildProps();
					wp.load(is);
					sshConfCache.put(path, wp);
					return wp;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	static String transform(String pattern, String name) {
		int n = pattern.indexOf('!');
		if (n < 0) {
			throw new IllegalArgumentException("Invalid host extractor [" + pattern + "]");
		}
		String format = pattern.substring(1, n);
		Matcher m = Pattern.compile(pattern.substring(n + 1)).matcher(name);
		if (!m.matches()) {
			throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
		}
		else {
			Object[] groups = new Object[m.groupCount()];
			for(int i = 0; i != groups.length; ++i) {
				groups[i] = m.group(i + 1);
				try {
					groups[i] = new Long((String)groups[i]);
				}
				catch(NumberFormatException e) {
					// ignore
				}				
			}
			try {
				return String.format(format, groups);
			}
			catch(IllegalArgumentException e) {
				throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
			}
		}
	}
	
	private static class SshSessionConfig {
		
		String host;
		String account;
		String password;
		String keyFile;
		String javaExec;
		String jarCachePath;
		
		public String toString() {
			return host + "|" + account + "|" + password + "|" + keyFile + "|" + javaExec + "|" + jarCachePath;
		}		
	}
	
	private static class SessionInfo {
		SshSessionConfig config;		
		SimpleSshJvmReplicator session;
		List<ViNode> processes = new ArrayList<ViNode>();
	}
}
