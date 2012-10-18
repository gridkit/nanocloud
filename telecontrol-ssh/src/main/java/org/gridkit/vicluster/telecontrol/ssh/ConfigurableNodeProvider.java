package org.gridkit.vicluster.telecontrol.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.isolate.IsolateViNodeProvider;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;

public class ConfigurableNodeProvider implements ViNodeProvider {

	private IsolateViNodeProvider isolateProvider;
	private ViNodeProvider localProvider;
	private ViNodeProvider remoteProvider;
	
	private Properties sshCredetials = new Properties();
	
	public void loadSshCredentials(String file) {
		Properties props = new Properties();
		String home = System.getProperty("user.home");
		File f = new File(new File(home), file);
		if (f.exists()) {
			try {
				FileInputStream fis = new FileInputStream(f);
				props.load(fis);
				try {
					fis.close();
				} catch (IOException e) {
					// ignore
				}
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
			setSshCredentials(props);
		}
		else {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
			if (is == null) {
				throw new IllegalArgumentException("No such file '" + file + "'");
			}
			else {
				try {
					props.load(is);
					try {
						is.close();
					} catch (IOException e) {
						// ignore
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				setSshCredentials(props);
			}
		}
	}
	
	public void setSshCredentials(Properties props) {
		this.sshCredetials = props;
	}
	
	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		String type = config.getProp(ViProps.NODE_TYPE);
		if (ViProps.NODE_TYPE_ISOLATE.equals(type)) {
			return getIsolateProvider().createNode(name, config);
		}
		else if (ViProps.NODE_TYPE_LOCAL.equals(type)) {
			return getLocalProvider().createNode(name, config);
		}
		else if (ViProps.NODE_TYPE_REMOTE.equals(type)) {
			return getRemoteProvider().createNode(name, config);
		}
		else {
			throw new IllegalArgumentException("Unknown node type: " + type);
		}
	}

	private synchronized ViNodeProvider getIsolateProvider() {
		if (isolateProvider == null) {
			isolateProvider = new IsolateViNodeProvider(); 
		}
		return isolateProvider;
	}

	private ViNodeProvider getLocalProvider() {
		if (localProvider == null) {
			localProvider = new JvmNodeProvider(new LocalJvmProcessFactory());
		}
		return localProvider;
	}
	
	private ViNodeProvider getRemoteProvider() {
		if (remoteProvider == null) {
			remoteProvider = new ConfigurableSshReplicator(new ConfigurableSshSessionProvider(sshCredetials));
		}
		return remoteProvider;
	}
}
