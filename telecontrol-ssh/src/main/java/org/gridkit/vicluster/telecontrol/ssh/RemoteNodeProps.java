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

import org.gridkit.nanocloud.ViConfigurable;
import org.gridkit.vicluster.ViNodeProps;

/**
 * Config properties for remote nodes.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteNodeProps implements ViNodeProps {

	/**
	 * Hint, where to execute process 
	 */
	public static String HOST = "remote:host";

	/**
	 * Hint, which account to use 
	 */
	public static String ACCOUNT = "remote:account";

	public static String PASSWORD = "remote:password";

	public static String SSH_AUTH_METHODS = "remote:ssh-auth-methods";

	public static String SSH_KEY_FILE = "remote:ssh-key-file";

	public static String SSH_CREDENTIAL_FILE = "remote:ssh-credential-file";

	/**
	 * Hint, remote path to java executable 
	 */	
	public static String JAVA_EXEC = "remote:java-exec";
	
	/**
	 * Hint. remote location for jar cache
	 */
	public static String JAR_CACHE_PATH = "remote:jar-cache-path"; 

	private ViConfigurable config;
	
	public static RemoteNodeProps at(ViConfigurable target) {
		return new RemoteNodeProps(target);
	}
	
	protected RemoteNodeProps(ViConfigurable target) {
		this.config = target;
	}

	public static void setRemoteHost(ViConfigurable config, String host) {
		config.setProp(HOST, host);
	}

	public RemoteNodeProps setRemoteHost(String host) {
		config.setProp(HOST, host);
		return this;
	}

	public static void setRemoteAccount(ViConfigurable config, String account) {
		config.setProp(ACCOUNT, account);
	}

	public RemoteNodeProps setRemoteAccount(String account) {
		config.setProp(ACCOUNT, account);
		return this;
	}

	public static void setSshConfig(ViConfigurable config, String path) {
		config.setProp(SSH_CREDENTIAL_FILE, path);
	}

	public RemoteNodeProps setSshConfig(String path) {
		config.setProp(SSH_CREDENTIAL_FILE, path);
		return this;
	}

	public static void setSshPrivateKey(ViConfigurable config, String path) {
		config.setProp(SSH_KEY_FILE, path);
	}
	
	public RemoteNodeProps setSshPrivateKey(String path) {
		config.setProp(SSH_KEY_FILE, path);
		return this;
	}
	
	public static void setRemoteJavaExec(ViConfigurable config, String javaExec) {
		config.setProp(JAVA_EXEC, javaExec);
	}

	public RemoteNodeProps setRemoteJavaExec(String javaExec) {
		config.setProp(JAVA_EXEC, javaExec);
		return this;
	}
	
	public static void setRemoteJarCachePath(ViConfigurable config, String jarCachePath) {
		config.setProp(JAR_CACHE_PATH, jarCachePath);
	}	

	public RemoteNodeProps setRemoteJarCachePath(String jarCachePath) {
		config.setProp(JAR_CACHE_PATH, jarCachePath);
		return this;
	}	
}
