package org.gridkit.vicluster.telecontrol.ssh;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathReplicator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathReplicator.class);
	private static final String DIGEST_ALGO = "SHA-1";
	
	private static WeakHashMap<ClassLoader, List<ClasspathEntry>> CLASSPATH_CACHE = new WeakHashMap<ClassLoader, List<ClasspathEntry>>();
	
	public static synchronized List<ClasspathEntry> getClasspath(ClassLoader classloader) {
		List<ClasspathEntry> classpath = CLASSPATH_CACHE.get(classloader);
		if (classpath == null) {
			classpath = new ArrayList<ClasspathReplicator.ClasspathEntry>();
			fillClasspath(classpath, ((URLClassLoader)classloader).getURLs());
			classpath = Collections.unmodifiableList(classpath);
			CLASSPATH_CACHE.put(classloader, classpath);
		}
		return classpath;
	}
	
	
	private static void fillClasspath(List<ClasspathEntry> classpath, URL[] urls) {
		for(URL url: urls) {
			ClasspathEntry entry = new ClasspathEntry();
			classpath.add(entry);
			entry.url = url;
			try {
				File file = new File(url.toURI());
				if (file.isFile()) {
					entry.file = file;
					entry.filename = file.getName();
				}
				else {
					String lname = file.getName();
					if ("classes".equals(lname)) {
						lname = file.getParentFile().getName();
					}
					if ("target".equals(lname)) {
						lname = file.getParentFile().getParentFile().getName();
					}
					lname += ".jar";
					entry.filename = lname;
					entry.data = ClasspathUtils.jarFiles(file.getPath());
				}
			}
			catch(Exception e) {
				LOGGER.warn("Cannot copy to remote host URL " + url.toString(), e);
				continue;
			}
		}		
	}


	public static class ClasspathEntry {
		
		private URL url;
		private String filename;
		private String hash;
		private File file;
		private byte[] data;
		
		public URL getUrl() {
			return url;
		}
		
		public synchronized String getHash() {
			if (hash == null) {
				hash = StreamHelper.digest(getData(), "SHA-1");
			}			
			return hash;
		}
		
		public String getFilename() {
			return filename;
		}
		
		public synchronized byte[] getData() {
			if (data != null) {
				return data;
			}
			else {
				// do not cache jar content in memory
				return StreamHelper.readFile(file);
			}
		}		
	}
	
}
