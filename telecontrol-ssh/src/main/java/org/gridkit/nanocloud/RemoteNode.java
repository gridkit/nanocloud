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
package org.gridkit.nanocloud;

import org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViExtender;

/**
 * Config properties for remote nodes.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteNode extends ViConfigurable.Delegate {

	public static ViExtender<RemoteNode> REMOTE = new ViExtender<RemoteNode>() {

		@Override
		public RemoteNode wrap(ViConfigurable node) {
			return RemoteNode.at(node);
		}		
	};
	
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

	/**
	 * Hint. remote location for jar cache
	 */
	public static String JAR_CACHE_PATH = "remote:jar-cache-path"; 

	private ViConfigurable config;
	
	public static RemoteNode at(ViConfigurable target) {
		return new RemoteNode(target);
	}
	
	protected RemoteNode(ViConfigurable target) {
		this.config = target;
	}

	@Override
	protected ViConfigurable getConfigurable() {
		return config;
	}
	
	public RemoteNode setRemoteNodeType() {
		config.setProp(ViConf.NODE_TYPE, ViConf.NODE_TYPE__REMOTE);
		return this;
	}

	public RemoteNode useSimpleRemoting() {
		setRemoteNodeType();
		setRemoteJarCachePath("/tmp/.nanocloud");
		config.setProp(SshSpiConf.REMOTE_FALLBACK_JVM_EXEC, "java");
		setHostsConfigFile("?~/ssh-credentials.prop");
		setRemoteHost("~%s!(.*)");

		return this;
	}
	
	public RemoteNode setRemoteHost(String host) {
		config.setProp(HOST, host);
		return this;
	}

	public RemoteNode setRemoteAccount(String account) {
		config.setProp(ACCOUNT, account);
		return this;
	}

	public RemoteNode setHostsConfigFile(String path) {
		config.setProp(ViConf.REMOTE_HOST_CONFIG, path);
		return this;
	}

	public RemoteNode setSshPrivateKey(String path) {
		config.setProp(SSH_KEY_FILE, path);
		return this;
	}
	
	public RemoteNode setRemoteJavaExec(String javaExec) {
		config.setProp(ViConf.JVM_EXEC_CMD, javaExec);
		return this;
	}
	
	public RemoteNode setRemoteJarCachePath(String jarCachePath) {
		config.setProp(SshSpiConf.REMOTE_JAR_CACHE, jarCachePath);
		return this;
	}	
}
