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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class DefaultSSHFactory implements SshSessionProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSSHFactory.class); 

	private JSch jsch = new JSch();
	
	private String user;
	private String password;
	private String passphrase;
	
	public DefaultSSHFactory() {
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setKeyFile(String fileName) {
		try {
			jsch.addIdentity(fileName);
		} catch (JSchException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	public Session getSession(String host) throws JSchException {
		UserInfo ui = new UserAuthnticator(host);
		
		int port = 22;
		if (host.indexOf(':') > 0) {
			String p = host.substring(host.lastIndexOf(':') + 1);
			host = host.substring(0, host.lastIndexOf(':'));
			port = Integer.parseInt(p);
		}
			
		Session session = jsch.getSession(user, host, port);
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
					logger.debug(message);
					break;
				case WARN: 
					logger.warn(message);
					break;
				case INFO: 
					logger.info(message);
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
