package org.gridkit.vicluster.telecontrol.ssh;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class SshRemotingConfig {

	private String host;
	private String account;
	private String javaExec;
	private String jarCache;
	
	private String password;
	private String keyfile;
	
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
}
