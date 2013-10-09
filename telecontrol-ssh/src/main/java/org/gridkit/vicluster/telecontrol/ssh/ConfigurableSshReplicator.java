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

import org.gridkit.vicluster.Hooks;
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
			
			RemoteJmvReplicator proto = getReplicatorProto(sc);
			
			String fp = proto.getFingerPrint();
			session = sessions.get(fp);
			
			if (session == null) {
				session = new SessionInfo();
				session.config = sc;
				sessions.put(fp, session);
			}
		}
		
		synchronized (session) {
			if (session.replicator == null) {
				session.replicator = getReplicatorProto(session.config);
				
				try {
					LOGGER.info("Establishing connection " + session.config.getConnectionSummary());
					session.replicator.init();
				} catch (Exception e) {
					session.replicator = null;
					throw new RuntimeException("SSH connection failed. Host [" + session.config.host + "] Error [" + e.getMessage() + "]", e);
				}
			}
			
			final SessionInfo context = session;
			final ViNode node = new JvmNodeProvider(session.replicator).createNode(name, effectiveConfig);			
			node.setConfigElement("hook:release-ssh", new Hooks.PostShutdownHook(new Runnable() {
				@Override
				public void run() {
					releaseConnection(context, node);
				}
			}));
			session.processes.add(node);
			
			return node;			
		}		
	}

	@Override
	public void shutdown() {
		// TODO implement shutdown()
	}

	private RemoteJmvReplicator getReplicatorProto(SshSessionConfig sc) {
//		RemoteJmvReplicator rep = new LegacySshJvmReplicator();
		RemoteJmvReplicator rep = new TunnellerJvmReplicator();
		rep.configure(sc.toConfig());
		return rep;
	}

	private void releaseConnection(SessionInfo session, ViNode connection) {
		synchronized(session) {
			session.processes.remove(connection);
			if (session.processes.isEmpty()) {
				LOGGER.info("Session " + session + " is not used");
				session.replicator.dispose();
				session.replicator = null;
			}
			else {
				return;
			}
		}
	}

	private synchronized SshSessionConfig resolveSsh(String name, ViNodeConfig nodeConfig) {
		SshSessionConfig s = new SshSessionConfig();
		s.host = nodeConfig.getProp(RemoteNodeProps.HOST);
		if (s.host == null) {
			throw new IllegalArgumentException("Remote host is not specified for node '" + name + "'");
		}
		if (s.host.startsWith("~")) {
			s.host = transform(s.host, name);
		}
		WildProps sshconf = getConf(nodeConfig.getProp(RemoteNodeProps.SSH_CREDENTIAL_FILE));
		if (sshconf != null) {
			s.account = sshconf.get(s.host);
			s.account = overrideUser(s.account, nodeConfig.getProp(RemoteNodeProps.ACCOUNT));
			s.password = sshconf.get(s.account + "@" + s.host + "!password");
			s.keyFile = sshconf.get(s.account + "@" + s.host + "!private-key");
			s.authMethods = sshconf.get(s.account + "@" + s.host + "!auth-methods");
			String hostOverride = sshconf.get(s.account + "@" + s.host + "!hostname");
			if (hostOverride != null) {
				s.host = hostOverride;
			}
		}
		
		s.account = overrideUser(s.account, nodeConfig.getProp(RemoteNodeProps.ACCOUNT));
		s.password = override(s.password, nodeConfig.getProp(RemoteNodeProps.PASSWORD));
		s.keyFile = override(s.keyFile, nodeConfig.getProp(RemoteNodeProps.SSH_KEY_FILE));
		s.javaExec = override(s.javaExec, nodeConfig.getProp(RemoteNodeProps.JAVA_EXEC));
		s.jarCachePath = override(s.jarCachePath, nodeConfig.getProp(RemoteNodeProps.JAR_CACHE_PATH));
		
		if (s.host == null) {
			throw new IllegalArgumentException("Remote host is not specified for node '" + name + "'");
		}
		
		if (s.account == null) {
			LOGGER.debug("Use default account for [" + name + "]");
			s.account = System.getProperty("user.name");
			if (s.account == null || s.account.trim().length() == 0) {
				throw new IllegalArgumentException("No account found for node '" + name + "'");
			}
		}
		
		if (s.password == null && s.keyFile == null) {
			if (s.account.equals(System.getProperty("user.name"))) {
				LOGGER.debug("Use default SSH keys [" + name + "]");
				s.keyFile = "~/.ssh/id_dsa|~/.ssh/id_rsa";
			}
			else {
				throw new IllegalArgumentException("No creadetials found for node '" + name + "'");
			}
		}

		if (s.javaExec == null) {
			throw new IllegalArgumentException("Java command is not specified for '" + name + "'");
		}

		if (s.jarCachePath == null) {
			throw new IllegalArgumentException("Jar cache location is not specified for '" + name + "'");
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

	private String overrideUser(String def, String override) {
		if (override != null) {
			return override;
		}
		else if (def == null){
			return System.getProperty("user.name");
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
				boolean optional = false;
				if (path.startsWith("?")) {
					optional = true;
					path = path.substring(1);
				}			
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
					if (optional) {
						LOGGER.info("SSH config [" + path + "] is not found");
						sshConfCache.put("?" + path, null);
						return null;
					}
					else {
						throw new RuntimeException(e);
					}
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
		String authMethods;
		String javaExec;
		String jarCachePath;
		
		public Map<String, String> toConfig() {
			Map<String, String> config = new HashMap<String, String>();
			config.put(RemoteNodeProps.HOST, host);
			config.put(RemoteNodeProps.ACCOUNT, account);
			config.put(RemoteNodeProps.PASSWORD, password);
			config.put(RemoteNodeProps.SSH_KEY_FILE, keyFile);
			config.put(RemoteNodeProps.SSH_AUTH_METHODS, authMethods);
			config.put(RemoteNodeProps.JAVA_EXEC, javaExec);
			config.put(RemoteNodeProps.JAR_CACHE_PATH, jarCachePath);
			return config;
		}
		
		public String getConnectionSummary() {
			return account + "@" + host + " - " 
					+ (keyFile == null ? "" : " pk-auth(" + keyFile +")") 
					+ (password == null ? "" : " password-auth");
		}
		
		public String toString() {
			return host + "|" + account + "|" + password + "|" + keyFile + "|" + javaExec + "|" + jarCachePath;
		}		
	}
	
	private static class SessionInfo {
		SshSessionConfig config;
		RemoteJmvReplicator replicator;
		List<ViNode> processes = new ArrayList<ViNode>();
	}
}
