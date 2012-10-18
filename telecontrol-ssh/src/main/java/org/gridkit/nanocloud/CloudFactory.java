package org.gridkit.nanocloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.gridkit.vicluster.ViHelper;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.WildProps;
import org.gridkit.vicluster.telecontrol.isolate.IsolateCloudFactory;
import org.gridkit.vicluster.telecontrol.ssh.ConfigurableNodeProvider;

public class CloudFactory {

	public static ViManager createIsolateCloud(String... packages) {
		return IsolateCloudFactory.createCloud(packages);
	}
	
	public static ViManager createLocalCloud(String configFile) {
		try {
			
			ConfigurableNodeProvider provider = new ConfigurableNodeProvider(true);
			ViManager cloud = new ViManager(provider);
			applyConfig(cloud, openStream(configFile));
			
			return cloud;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	public static ViManager createLocalCloud(Reader configReader) {
		ConfigurableNodeProvider provider = new ConfigurableNodeProvider(true);
		ViManager cloud = new ViManager(provider);
		applyConfig(cloud, configReader);
		
		return cloud;
	}
	
	public static ViManager createSshCloud(String configFile) {
		try {
			
			ConfigurableNodeProvider provider = new ConfigurableNodeProvider(false);
			ViManager cloud = new ViManager(provider);
			applyConfig(cloud, openStream(configFile));
			
			return cloud;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}				
	}

	public static ViManager createSshCloud(Reader configReader) {
		ConfigurableNodeProvider provider = new ConfigurableNodeProvider(false);
		ViManager cloud = new ViManager(provider);
		applyConfig(cloud, configReader);
		
		return cloud;		
	}
	
	public static void applyConfig(ViManager manager, Reader reader) {
		WildProps wp = new WildProps();
		try {
			wp.load(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ViHelper.configure(manager, wp.entryList());
	}

	public static void applyConfig(ViManager manager, InputStream is) {
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
