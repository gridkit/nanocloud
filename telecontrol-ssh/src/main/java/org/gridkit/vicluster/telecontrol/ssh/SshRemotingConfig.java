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

import java.util.Map;

class SshRemotingConfig {

	private String host;
	private String account;
	private String javaExec;
	private String jarCache;
	private long tunnelerTimeout;
	
	private String password;
	private String keyfile;
	private String authMethods;
	
	public SshRemotingConfig() {		
	}
	
	public void configure(Map<String, String> props) {
		if (props.containsKey(RemoteNodeProps.HOST)) {
			host = props.get(RemoteNodeProps.HOST);
		}
		if (props.containsKey(RemoteNodeProps.ACCOUNT)) {
			account = props.get(RemoteNodeProps.ACCOUNT);
		}
		if (props.containsKey(RemoteNodeProps.JAVA_EXEC)) {
			javaExec = props.get(RemoteNodeProps.JAVA_EXEC);
		}
		if (props.containsKey(RemoteNodeProps.JAR_CACHE_PATH)) {
			jarCache = props.get(RemoteNodeProps.JAR_CACHE_PATH);
		}
		if (props.containsKey(RemoteNodeProps.SSH_KEY_FILE)) {
			keyfile = props.get(RemoteNodeProps.SSH_KEY_FILE);
		}
		if (props.containsKey(RemoteNodeProps.PASSWORD)) {
			password = props.get(RemoteNodeProps.PASSWORD);
		}
		if (props.containsKey(RemoteNodeProps.SSH_AUTH_METHODS)) {
			authMethods = props.get(RemoteNodeProps.SSH_AUTH_METHODS);
		}
		if (props.containsKey(RemoteNodeProps.SSH_TUNNELER_TIMEOUT)) {
		    tunnelerTimeout = Long.valueOf(props.get(RemoteNodeProps.SSH_TUNNELER_TIMEOUT));
		}
	}
	
	public void validate() {
		if (host == null) {
			throw new IllegalArgumentException("Host is not specified");
		}
		if (account == null) {
			throw new IllegalArgumentException("Account is not specified");
		}
		if (javaExec == null) {
			throw new IllegalArgumentException("Java command is not specified");
		}
		if (jarCache == null) {
			throw new IllegalArgumentException("Remote jar cache path is not specified");
		}
		if (password == null && keyfile == null) {
			throw new IllegalArgumentException("SSH credentials are missing");
		}
// TODO SSH key file validator
//		if (keyfile != null) {
//			String kf = keyfile;
//			if (kf.startsWith("~/")) {
//				try {
//					kf = new File(new File(System.getProperty("user.home")), kf.substring(2)).getCanonicalPath();
//				} catch (IOException e) {
//					// ignore;
//				} 
//			}
//			if (!new File(kf).exists()) {
//				throw new IllegalArgumentException("SSH key file \"" + kf + "\" is missing");
//			}
//		}
	}
	
	public String getFingerPrint() {
		StringBuilder sb = new StringBuilder();
		sb.append("host").append(":").append(host).append("|");
		sb.append("account").append(":").append(account).append("|");
		sb.append("javaExec").append(":").append(javaExec).append("|");
		sb.append("jarCachePath").append(":").append(jarCache);
		
		return sb.toString();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getJavaExec() {
		return javaExec;
	}

	public void setJavaExec(String javaExec) {
		this.javaExec = javaExec;
	}

	public String getJarCachePath() {
		return jarCache;
	}

	public void setJarCachePath(String jarCache) {
		this.jarCache = jarCache;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getKeyFile() {
		return keyfile;
	}

	public void setKeyFile(String keyfile) {
		this.keyfile = keyfile;
	}

	public String getAuthMethods() {
		return authMethods;
	}

	public void setAuthMethods(String authMethods) {
		this.authMethods = authMethods;
	}

    public long getTunnellerTimeout() {
        return tunnelerTimeout;
    }

    public void setTunnellerTimeout(long tunnellerTimeout) {
        this.tunnelerTimeout = tunnellerTimeout;
    }
}
