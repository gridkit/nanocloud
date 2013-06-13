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
package org.gridkit.vicluster.telecontrol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Classpath {

	private static final Logger LOGGER = LoggerFactory.getLogger(Classpath.class);
	private static final String DIGEST_ALGO = "SHA-1";
	
	private static WeakHashMap<ClassLoader, List<ClasspathEntry>> CLASSPATH_CACHE = new WeakHashMap<ClassLoader, List<ClasspathEntry>>();
	
	public static synchronized List<ClasspathEntry> getClasspath(ClassLoader classloader) {
		List<ClasspathEntry> classpath = CLASSPATH_CACHE.get(classloader);
		if (classpath == null) {
			classpath = new ArrayList<Classpath.ClasspathEntry>();
			fillClasspath(classpath, ClasspathUtils.listCurrentClasspath(((URLClassLoader)classloader)));
			classpath = Collections.unmodifiableList(classpath);
			CLASSPATH_CACHE.put(classloader, classpath);
		}
		return classpath;
	}
	
	
	private static void fillClasspath(List<ClasspathEntry> classpath, Collection<URL> urls) {
		for(URL url: urls) {
			ClasspathEntry entry = new ClasspathEntry();
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
					if (entry.data == null) {
						LOGGER.warn("Classpath entry is empty: " + file.getCanonicalPath());
						continue;
					}
				}
				classpath.add(entry);
			}
			catch(Exception e) {
				LOGGER.warn("Cannot copy URL content: " + url.toString(), e);
				continue;
			}
		}		
	}

	public static class ClasspathEntry implements FileBlob {
		
		private URL url;
		private String filename;
		private String hash;
		private File file;
		private byte[] data;
		
		public URL getUrl() {
			return url;
		}
		
		@Override
		public String getFileName() {
			return filename;
		}

		@Override
		public synchronized String getContentHash() {
			if (hash == null) {
				hash = StreamHelper.digest(getData(), DIGEST_ALGO);
			}			
			return hash;
		}

		@Override
		public synchronized InputStream getContent() {
			try {
				return (InputStream) (data != null ? new ByteArrayInputStream(data) : new FileInputStream(file));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		@Override
		public long size() {
			return data != null ? data.length : file.length();
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
