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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Configurable SSH session provider capable of choosing different credentials for hosts.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 */
public class ConfigurableSshSessionProvider implements SshSessionProvider {
	
	private static final String CFG_DEFAULT_PROFILE = "default-profile";
	private static final String CFG_LOGIN = "login";
	private static final String CFG_PASSWORD = "password";
	private static final String CFG_KEY_FILE = "key-file";
	
	private List<ConfigEntry> entries = new ArrayList<ConfigEntry>();
	
	@Override
	public Session getSession(String host, String account) throws JSchException {
		if (account == null) {
			String user = findDefaultAccount(host);
			if (user == null) {
				throw new IllegalArgumentException("No default account found for host " + host);
			}
			return getSession(host, user);
		}
		
		Map<String, String> config = resolveConfig(host, account);
		SimpleSshSessionProvider simpleProvider = new SimpleSshSessionProvider();
		if (config.containsKey(CFG_LOGIN)) {
			simpleProvider.setUser(config.get(CFG_LOGIN));
		}
		else {
			simpleProvider.setUser(account);
		}
		if (config.containsKey(CFG_PASSWORD)) {
			simpleProvider.setPassword(config.get(CFG_PASSWORD));
		}
		if (config.containsKey(CFG_KEY_FILE)) {
			simpleProvider.setKeyFile(config.get(CFG_KEY_FILE));
		}
		
		return simpleProvider.getSession(host, null);
	}

	private Map<String, String> resolveConfig(String host, String account) {
		if (host.lastIndexOf(':') >= 0) {
			// ignore port for now
			host = host.substring(0, host.lastIndexOf(':'));
		}
		Map<String, String> config = new LinkedHashMap<String, String>();
		for (ConfigEntry entry: entries) {
			if (entry.matches(host, account)) {
				config.put(entry.getConfigProp(), entry.getConfigValue());
			}
		}
		return config;
	}

	private String findDefaultAccount(String host) {
		String user = null;
		for(ConfigEntry entry: entries) {
			if (entry.matches(host, null) && CFG_DEFAULT_PROFILE.equals(entry.getConfigProp())) {
				user = entry.getConfigValue();
			}
		}
		return user;
	}

	public SshConfig hosts(String pattern) {
		return new SshConfigBuilder(pattern);
	}
	
	public static interface SshConfig {
		
		public SshConfig profile(String account);
		
		public SshConfig defaultProfile();
		
		public SshConfig defaultProfile(String account);
		
		public SshConfig useLogin(String login);

		public SshConfig usePassword(String password);
		
		public SshConfig usePrivateKey(String path);
		
	}

	private class SshConfigBuilder implements SshConfig {
		
		private String hostPattern;
		private String profile = null;
		
		public SshConfigBuilder(String hostPattern) {
			this.hostPattern = hostPattern;
		}

		@Override
		public SshConfig profile(String account) {
			profile = account;
			return this;
		}

		@Override
		public SshConfig defaultProfile() {
			if (profile == null) {
				throw new IllegalArgumentException("Profile name is not set");
			}
			entries.add(new ConfigEntry(hostPattern, null, CFG_DEFAULT_PROFILE, profile));
			return this;
		}

		@Override
		public SshConfig defaultProfile(String account) {
			entries.add(new ConfigEntry(hostPattern, null, CFG_DEFAULT_PROFILE, account));
			return this;
		}

		@Override
		public SshConfig useLogin(String login) {
			if (profile == null) {
				throw new IllegalArgumentException("Profile name is not set");
			}
			entries.add(new ConfigEntry(hostPattern, profile, CFG_LOGIN, login));
			return this;
		}

		@Override
		public SshConfig usePassword(String password) {
			if (profile == null) {
				throw new IllegalArgumentException("Profile name is not set");
			}
			entries.add(new ConfigEntry(hostPattern, profile, CFG_PASSWORD, password));
			return this;
		}

		@Override
		public SshConfig usePrivateKey(String path) {
			if (profile == null) {
				throw new IllegalArgumentException("Profile name is not set");
			}
			entries.add(new ConfigEntry(hostPattern, profile, CFG_KEY_FILE, path));
			return this;
		}
	}
	
	private static class ConfigEntry {
		
		@SuppressWarnings("unused")
		private String hostPattern;
		private Pattern compiledPattern;
		private String user;
		private String configProp;
		private String value;
		
		public ConfigEntry(String hostPattern, String user, String configProp, String value) {
			this.hostPattern = hostPattern;
			this.user = user;
			this.configProp = configProp;
			this.value = value;
			this.compiledPattern = GlobHelper.translate(hostPattern, ".");
		}
		
		public boolean matches(String host, String user) {
			if (this.user != null && !this.user.equals(user)) {
				return false;
			}
			if (this.user == null && user != null) {
				return false;
			}
			return compiledPattern.matcher(host).matches();
		}
		
		public String getConfigProp() {
			return configProp;
		}
		
		public String getConfigValue() {
			return value;
		}
	}
}
