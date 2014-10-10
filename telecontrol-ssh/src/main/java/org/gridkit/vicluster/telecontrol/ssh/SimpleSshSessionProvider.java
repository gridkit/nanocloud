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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.internal.com.jcraft.jsch.JSch;
import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.internal.com.jcraft.jsch.UIKeyboardInteractive;
import org.gridkit.internal.com.jcraft.jsch.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SimpleSshSessionProvider implements SshSessionFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSshSessionProvider.class); 

	private JSch jsch;
	
	private String user;
	private String password;
	private String passphrase;
	private Map<String, String> config = new HashMap<String, String>();
	
	public SimpleSshSessionProvider() {
		this(new JSch());
	}

	public SimpleSshSessionProvider(JSch jsch) {
		this.jsch = jsch;
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setKeyFile(String fileName) {
		boolean added = false;
		String[] paths = fileName.split("[|]");
		for(String path: paths) {
			try {
				if (path.startsWith("~/")) {
					try {
						path = new File(System.getProperty("user.home"), path.substring(2)).getCanonicalPath();
					} catch (IOException e) {
						// ignore
					}
				}
				File f = new File(path);
				if (f.exists()) {
					jsch.addIdentity(path);
					added = true;
				}
			} catch (JSchException e) {
				throw new IllegalArgumentException(e);
			}
		}
		if (!added) {
			throw new IllegalArgumentException("No keys found at [" + fileName + "]");
		}
	}
	
	public void setConfig(String key, String value) {
		config.put(key, value);
	}
	
	@Override
	public Session getSession(String host, String account) throws JSchException {
		if (account != null && !user.equals(account)) {
			throw new IllegalArgumentException("User '" + account + "' is not configured");
		}
		
		UserInfo ui = new UserAuthnticator(host);
		
		int port = 22;
		if (host.indexOf(':') > 0) {
			String p = host.substring(host.lastIndexOf(':') + 1);
			host = host.substring(0, host.lastIndexOf(':'));
			port = Integer.parseInt(p);
		}
				
		Session session = jsch.getSession(user, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
        if (password != null) {
            session.setPassword(password);
        }
        for(String key: config.keySet()) {
			session.setConfig(key, config.get(key));
		}
		session.setDaemonThread(true);
		session.setUserInfo(ui);		
		session.connect();
		
		return session;
	}

	static {
		JSch.setLogger(new JSchLogger());
	}
	
	private final class UserAuthnticator implements UserInfo, UIKeyboardInteractive {
		private final String host;

		private UserAuthnticator(String host) {
			this.host = host;
		}

		@Override
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
			LOGGER.debug("[" + host + "] SSH: keyboard-interactive Prompt=" + Arrays.toString(prompt));
			return new String[]{password != null ? password : ""};
		}

		@Override
		public void showMessage(String message) {
			LOGGER.debug("[" + host + "] SSH: " + message);
		}

		@Override
		public boolean promptYesNo(String message) {
			LOGGER.debug("[" + host + "] SSH: " + message + " <- yes");
			return true;
		}

		@Override
		public boolean promptPassword(String message) {
			LOGGER.debug("[" + host + "] SSH: " + message + " <- yes");
			return true;
		}

		@Override
		public boolean promptPassphrase(String message) {
			LOGGER.debug("[" + host + "] SSH: " + message + " <- yes");
			return true;
		}

		@Override
		public String getPassword() {
			LOGGER.debug("[" + host + "] SSH: password = " + password);
			return password;
		}

		@Override
		public String getPassphrase() {
			LOGGER.debug("[" + host + "] SSH: passphrase = " + password);
			return passphrase;
		}
	}

	private final static class JSchLogger implements org.gridkit.internal.com.jcraft.jsch.Logger {
		
		private org.slf4j.Logger logger;
		
		public JSchLogger() {
			logger = LoggerFactory.getLogger("remoting.ssh.jsch");
		}
		
		@Override
		public void log(int level, String message) {
			switch(level) {
				case DEBUG: 
					logger.trace(message);
					break;
				case WARN: 
					if (message.endsWith("(RSA) to the list of known hosts.")) {
						logger.info(message);
					}
					else {
						logger.warn(message);
					}
					break;
				case INFO: 
					logger.debug(message);
					break;
				case ERROR: 
					logger.error(message);
					break;
				case FATAL: 
					logger.error(message);
					break;
				default:
					logger.warn(message);
			}			
		}
	
		@Override
		public boolean isEnabled(int level) {
			switch(level) {
			case DEBUG: 
				return logger.isDebugEnabled();
			case WARN: 
				return logger.isWarnEnabled();
			case INFO: 
				return logger.isInfoEnabled();
			case ERROR: 
				return logger.isErrorEnabled();
			case FATAL: 
				return logger.isErrorEnabled();
			default: 
				return logger.isWarnEnabled();
			}
		}
	}	
}
