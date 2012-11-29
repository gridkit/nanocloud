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
import java.util.Map;

import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.JvmConfig;

public class LegacySshJvmReplicator implements RemoteJmvReplicator {

	public static String HOST = RemoteNodeProps.HOST;
	public static String ACCOUNT = RemoteNodeProps.ACCOUNT;
	public static String PASSWORD = RemoteNodeProps.PASSWORD;
	public static String SSH_KEY_FILE = RemoteNodeProps.SSH_KEY_FILE;
	public static String JAVA_EXEC = RemoteNodeProps.JAVA_EXEC;
	public static String JAR_CACHE_PATH = RemoteNodeProps.JAR_CACHE_PATH;
	
	private String host;
	private String account;
	private String password;
	private String sshKeyFile;
	private String javaExec;
	private String jarCachePath;
	
	private SimpleSshJvmReplicator replicator;

	@Override
	public ControlledProcess createProcess(String caption, JvmConfig jvmArgs) throws IOException {
		if (replicator == null) {
			throw new IllegalStateException("Not initialized");
		}
		return replicator.createProcess(caption, jvmArgs);
	}

	@Override
	public void configure(Map<String, String> nodeConfig) {
		
		host = nodeConfig.get(HOST);
		account = nodeConfig.get(ACCOUNT);
		javaExec = nodeConfig.get(JAVA_EXEC);
		jarCachePath = nodeConfig.get(JAR_CACHE_PATH);
		password = nodeConfig.get(PASSWORD);
		sshKeyFile = nodeConfig.get(SSH_KEY_FILE);
		
		if (host == null || account == null || javaExec == null || jarCachePath == null) {
			throw new IllegalArgumentException("Insufficient configuration");
		}
		if (password == null && sshKeyFile == null) {
			throw new IllegalArgumentException("Missing SSH credentials");
		}
	}
	
	@Override
	public String getFingerPrint() {
		StringBuilder sb = new StringBuilder();
		sb.append("host").append(":").append(host).append("|");
		sb.append("account").append(":").append(account).append("|");
		sb.append("javaExec").append(":").append(javaExec).append("|");
		sb.append("jarCachePath").append(":").append(jarCachePath);
		
		return sb.toString();
	}

	@Override
	public void init() throws Exception {
		
		SimpleSshSessionProvider sf = new SimpleSshSessionProvider();
		sf.setUser(account);
		if (sshKeyFile != null) {
			sf.setKeyFile(sshKeyFile);
		}
		else if (password != null){
			sf.setPassword(password);
		}
		replicator = new SimpleSshJvmReplicator(host, account, sf);
		replicator.setJavaExecPath(javaExec);
		replicator.setAgentHome(jarCachePath);
		replicator.init();
	}

	@Override
	public void dispose() {
		replicator.shutdown();		
	}
}
