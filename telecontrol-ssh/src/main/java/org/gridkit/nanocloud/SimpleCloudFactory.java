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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViHelper;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.WildProps;
import org.gridkit.vicluster.telecontrol.ssh.RemoteNodeProps;

/**
 * This is substitute for old CloudFactory class. 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SimpleCloudFactory {

	/**
	 * Create {@link Cloud} with configuration driven node provider.
	 * @see #applyConfig(ViManager, String)
	 * @return unconfigured instance of {@link ViManager}
	 */
	public static Cloud createCloud() {		
		return CloudFactory.createCloud();
	}
	
	/**
	 * Create {@link ViManager} with configuration driven node provider.
	 * @see #applyConfig(ViManager, String)
	 * @return configured instance of {@link ViManager}
	 */
	public static Cloud createCloud(String configFile) {
		try {
			
			Cloud cloud = createCloud();
			applyConfig(cloud, openStream(configFile));
			
			return cloud;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}				
	}

	/**
	 * Create {@link ViManager} with configuration driven node provider.
	 * @see #applyConfig(ViManager, String)
	 * @return configured instance of {@link ViManager}
	 */
	public static Cloud createCloud(Reader configReader) {
		Cloud cloud = createCloud();
		applyConfig(cloud, configReader);
		
		return cloud;		
	}

	/**
	 * Creates instance of configuration driven {@link ViManager}, configured to interpret {@link ViNode}'s name as hostname.
	 * <br/>
	 * Default SSH configuration is used.
	 * If password-less SSH is set up in environment, this {@link ViManager} cloud be used without further configuration.
	 * <br/>
	 * If present, <code>~/ssh-credentials.prop</code> would be used for SSH credentials lookup. 
	 * @return configured instance of {@link ViManager}
	 */
	public static Cloud createSimpleSshCloud() {
		Cloud cloud = createCloud();
		ViProps.at(cloud.node("**")).setRemoteType();
		cloud.node("**").setProp(ViConf.JVM_EXEC_CMD, "java");
		cloud.node("**").setProp(SshSpiConf.SPI_JAR_CACHE, "/tmp/.telecontrol");
		cloud.node("**").setProp(ViConf.REMOTE_HOST_CONFIG, "?~/ssh-credentials.prop");
		RemoteNodeProps.at(cloud.node("**")).setRemoteHost("~%s!(.*)");
		
		return cloud;
	}
	
	public static void applyConfig(ViManager manager, String config) {
		try {
			applyConfig(manager, openStream(config));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void applyConfig(Cloud manager, Reader reader) {
		WildProps wp = new WildProps();
		try {
			wp.load(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ViHelper.configure(manager, wp.entryList());
	}

	public static void applyConfig(Cloud manager, InputStream is) {
		WildProps wp = new WildProps();
		try {
			wp.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ViHelper.configure(manager, wp.entryList());
	}
	
	
	private static InputStream openStream(String path) throws IOException {
		InputStream is = null;
		if (path.startsWith("~/")) {
			String userHome = System.getProperty("user.home");
			File cpath = new File(new File(userHome), path.substring(2));
			is = new FileInputStream(cpath);
		}
		else if (path.startsWith("resource:")) {
			String rpath = path.substring("resource:".length());
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			is = cl.getResourceAsStream(rpath);
			if (is == null) {
				throw new FileNotFoundException("Resource not found '" + path + "'");
			}
		}
		else {
			if (new File(path).exists()) {
				is = new FileInputStream(new File(path));
			}
			else {
				try {
					is = new URL(path).openStream();
				}
				catch(IOException e) {
					// ignore
				}
				if (is == null) {
					throw new FileNotFoundException("Cannot resolve path '" + path + "'");
				}
			}
		}
		return is;	
	}	
}
