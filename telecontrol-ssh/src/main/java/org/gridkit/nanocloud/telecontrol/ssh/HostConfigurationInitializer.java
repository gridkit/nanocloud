package org.gridkit.nanocloud.telecontrol.ssh;

import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_ADDRESS;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_JAR_CACHE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_JAVA_EXEC;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_JSCH_PREFERED_AUTH;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_PASSWORD;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_PRIVATE_KEY_FILE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_JAR_CACHE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_JSCH_OPTION;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_PASSWORD;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_PRIVATE_KEY_FILE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_TARGET_ACCOUNT;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_TARGET_HOST;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.ViEngine.Rerun;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.WildProps;

public class HostConfigurationInitializer implements Interceptor {

	@Override
	public void process(String name, Phase phase, QuorumGame game) {
		if (phase == Phase.PRE_INIT) {
			game.rerunOnQuorum(new Runner());
		}		
	}

	@Override
	public void processAdHoc(String name, ViExecutor node) {
		throw new IllegalStateException("Node is already initialized");
	}
	
	static class Runner implements Rerun {

		boolean first = true;
		
		@Override
		public void rerun(QuorumGame game, Map<String, Object> changes) {
			if (first || changes.containsKey(ViConf.REMOTE_HOST) 
					|| changes.containsKey(ViConf.REMOTE_ACCOUNT)
					|| changes.containsKey(ViConf.REMOTE_HOST_CONFIG)) {
				first = false;
				applyHostConfig(game);
				game.rerunOnUpdate(this);
			}
		}

		private void set(QuorumGame game, String key, Object value) {
			Object old = game.getProp(key);
			if (old == null && value == null) {
				return;
			}
			else if (value != null && value.equals(old)) {
				return;
			}
			game.setProp(key, value);
		}
		
		public void applyHostConfig(QuorumGame game) {
			String rhost = game.get(ViConf.REMOTE_HOST);
			if (rhost == null) {
				return;
			}
			String host = ViEngine.Core.transform(rhost, game.getNodeName());
			Map<String, Object> ec = new HashMap<String, Object>();
			ec.put(SPI_SSH_TARGET_HOST, host);
			ec.put(SPI_SSH_TARGET_ACCOUNT, game.get(ViConf.REMOTE_ACCOUNT));
			
			String config = game.get(ViConf.REMOTE_HOST_CONFIG);
			if (config != null) {
				try {
					InputStream is = ViEngine.Core.openStream(config);
					if (is != null) {
						WildProps wp = new WildProps();
						wp.load(is);
						processHostConfig(host, (String)game.get(ViConf.REMOTE_ACCOUNT), wp, ec);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			if (game.get(RemoteNode.PASSWORD) != null) {
			    ec.put(SPI_SSH_PASSWORD, game.get(RemoteNode.PASSWORD));
			}
			if (game.get(SshSpiConf.SSH_PASSWORD) != null) {
			    ec.put(SPI_SSH_PASSWORD, game.get(SshSpiConf.SSH_PASSWORD));
			}
			
			if (ec.get(SPI_SSH_TARGET_ACCOUNT) == null) {
			    ec.put(SPI_SSH_TARGET_ACCOUNT, System.getProperty("user.name"));
			}
			if (ec.get(SPI_SSH_PASSWORD) == null && ec.get(SPI_SSH_PRIVATE_KEY_FILE) == null) {
			    ec.put(SPI_SSH_PRIVATE_KEY_FILE, "~/.ssh/id_dsa|~/.ssh/id_rsa");
			}
			
			for(String key: ec.keySet()) {
				if (ec.get(key) != null) {
					set(game, key, ec.get(key));
				}
			}
		}

		protected void processHostConfig(String host, String account, WildProps wp, Map<String, Object> ec) {
			if (account == null) {
				account = wp.get(host);
				ec.put(SPI_SSH_TARGET_ACCOUNT, account);
			}
			if (account == null) {
				throw new RuntimeException("Cannot resolve remote account");
			}
			String key = account + "@" + host;
			ec.put(SPI_SSH_PASSWORD, wp.get(key + "!" + KEY_PASSWORD));
			ec.put(SPI_SSH_PRIVATE_KEY_FILE, wp.get(key + "!" + KEY_PRIVATE_KEY_FILE));
			if (wp.get(key + "!" + KEY_ADDRESS) != null) {
				ec.put(SPI_SSH_TARGET_HOST, wp.get(key + "!" + KEY_ADDRESS));
			}
			ec.put(SPI_BOOTSTRAP_JVM_EXEC, wp.get(key + "!" + KEY_JAVA_EXEC));
			ec.put(SPI_JAR_CACHE, wp.get(key + "!" + KEY_JAR_CACHE));
			ec.put(SPI_SSH_JSCH_OPTION + "PreferredAuthentications", wp.get(key + "!" + KEY_JSCH_PREFERED_AUTH));
		}
	}
}
