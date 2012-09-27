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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SimpleSshSessionProvider implements SshSessionFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSshSessionProvider.class); 

	private JSch jsch;
	
	private String user;
	private String password;
	private String passphrase;
	
	public SimpleSshSessionProvider() {
		this(new JSch());
	}

	public SimpleSshSessionProvider(JSch jsch) {
		this.jsch = jsch;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setKeyFile(String fileName) {
		try {
			File f = new File(fileName);
			if (!f.exists()) {
				// Try to lookup files in home directory
				File home = new File(System.getProperty("user.home"));
				if (new File(home, fileName).exists()) {
					try {
						fileName = new File(home, fileName).getCanonicalPath();
					} catch (IOException e) {
						// ignore
					}
				}
			}
			jsch.addIdentity(fileName);
		} catch (JSchException e) {
			throw new IllegalArgumentException(e);
		}
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
			return new String[]{password};
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

	private final static class JSchLogger implements com.jcraft.jsch.Logger {
		
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
