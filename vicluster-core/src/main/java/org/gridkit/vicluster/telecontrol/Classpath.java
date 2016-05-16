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
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Classpath {

	private static final Logger LOGGER = LoggerFactory.getLogger(Classpath.class);
	private static final String DIGEST_ALGO = "SHA-1";
	
	private static WeakHashMap<ClassLoader, List<ClasspathEntry>> CLASSPATH_CACHE = new WeakHashMap<ClassLoader, List<ClasspathEntry>>();
	private static WeakHashMap<URL, WeakReference<ClasspathEntry>> CUSTOM_ENTRIES = new WeakHashMap<URL, WeakReference<ClasspathEntry>>();
	
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
	
	public static synchronized ClasspathEntry getLocalEntry(String path) throws IOException {
		try {
			URL url = new File(path).toURI().toURL();
			WeakReference<ClasspathEntry> wr = CUSTOM_ENTRIES.get(url);
			if (wr != null) {
				ClasspathEntry entry = wr.get();
				return entry;
			}
			ClasspathEntry entry = newEntry(url);
			CUSTOM_ENTRIES.put(url, new WeakReference<ClasspathEntry>(entry));
			return entry;
		} catch (MalformedURLException e) {
			throw new IOException(e);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public static synchronized FileBlob createBinaryEntry(String name, byte[] data) {
		return new ByteBlob(name, data);
	}
	
	private static void fillClasspath(List<ClasspathEntry> classpath, Collection<URL> urls) {
		for(URL url: urls) {
			if (!isIgnoredJAR(url)) {
				try {
					ClasspathEntry entry = newEntry(url);
					if (entry == null) {
						LOGGER.warn("Cannot copy URL content: " + url.toString());
						continue;
					}
					classpath.add(entry);
				}
				catch(Exception e) {
					LOGGER.warn("Cannot copy URL content: " + url.toString(), e);
					continue;
				}
			}
		}		
	}

	private static boolean isIgnoredJAR(URL url) {
		try {
			if ("file".equals(url.getProtocol())) {
				File f = new File(url.toURI());
				if (f.isFile()) {
					if (belongs(JRE_ROOT, url)) {
						// ignore JRE based jars, e.g. tools.jar
						return true;
					}
					else if (f.getName().startsWith("surefire") && isManifestOnly(f)) {
						// surefirebooter will interfere with classpath tweaking, exclude it
						return true;
					}
				}
			}
		} catch (URISyntaxException e) {
			// ignore
		}
		return false;
	}

	private static boolean isManifestOnly(File f) {
		JarFile jar = null;
		try {
			jar = new JarFile(f);
			Enumeration<JarEntry> en = jar.entries();
			if (!en.hasMoreElements()) {
				return false;
			}
			JarEntry je = en.nextElement();
			if ("META-INF/".equals(je.getName())) {
				if (!en.hasMoreElements()) {
					return false;
				}
				je = en.nextElement();
			}		
			if (!"META-INF/MANIFEST.MF".equals(je.getName())) {
				return false;
			}		
			return !en.hasMoreElements();
		} catch (IOException e) {
			return false;			
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private static URL JRE_ROOT = getJreRoot();
	
	private static boolean belongs(URL base, URL derived) {
		// TODO not exactly correct, but should work
		return derived.toString().startsWith(base.toString());				
	}
	
	private static URL getJavaHome() throws MalformedURLException {
		return new File(System.getProperty("java.home")).toURI().toURL();			
	}

	// See Isolate
	private static URL getJreRoot() {
		try {
			String jlo = ClassLoader.getSystemResource("java/lang/Object.class").toString();
			String root = jlo;
			if (root.startsWith("jar:")) {
				root = root.substring("jar:".length());
				int n = root.indexOf('!');
				root = root.substring(0, n);
				
				if (root.endsWith("/rt.jar")) {
					root = root.substring(0, root.lastIndexOf('/'));
					if (root.endsWith("/lib")) {
						root = root.substring(0, root.lastIndexOf('/'));
						return new URL(root);
					}
				}
			}
			// fall back
			return getJavaHome();
		}
		catch(MalformedURLException e) {
			return null;
		}
	}
	
	private static ClasspathEntry newEntry(URL url) throws IOException, URISyntaxException {
		ClasspathEntry entry = new ClasspathEntry();
		entry.url = url;
		File file = uriToFile(url.toURI());
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
			if (lname.startsWith(".")) {
				lname = lname.substring(1);
			}
			lname += ".jar";
			entry.file = file;
			entry.filename = lname;
			if (isEmpty(file)) {
				LOGGER.warn("Classpath entry is empty: " + file.getCanonicalPath());
				return null;
			}
			entry.lazyJar = true;
		}
		return entry;
	}
	
	private static boolean isEmpty(File file) {
		File[] files = file.listFiles();
		if (files == null) {
			return true;
		}
		else {
			for(File c: files) {
				if (c.isFile() || !isEmpty(c)) {
					return false;
				}
			}
		}
		return true;
	}

	private static File uriToFile(URI uri) {
		if ("file".equals(uri.getScheme())) {
			if (uri.getAuthority() == null) {
				return new File(uri);
			}
			else {
				// try to fix broken windows network path
				String path = "file:////" + uri.getAuthority() + "/" + uri.getPath();
				try {
					return new File(new URI(path));
				} catch (URISyntaxException e) {
					return new File(uri);
				}
			}
		}
		return new File(uri);
	}

	public static class ClasspathEntry implements FileBlob {
		
		private URL url;
		private String filename;
		private String hash;
		private File file;
		private boolean lazyJar;
		private byte[] data;
		private Boolean isGridKitClasses;
		private Boolean isTestClasses;

		public URL getUrl() {
			return url;
		}
		
		@Override
		public File getLocalFile() {
			return file;
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
			ensureData();
			try {
				return (InputStream) (data != null ? new ByteArrayInputStream(data) : new FileInputStream(file));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		private synchronized void ensureData() {
			if (lazyJar) {
				try {
					data = ClasspathUtils.jarFiles(file.getPath());
					lazyJar = false;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}			
		}

		@Override
		public long size() {
			ensureData();
			return data != null ? data.length : file.length();
		}

		public synchronized byte[] getData() {
			ensureData();
			if (data != null) {
				return data;
			}
			else {
				// do not cache jar content in memory
				return StreamHelper.readFile(file);
			}
		}		
		
		public String toString() {
			return filename;
		}

		public synchronized boolean isGridKitClasses() {
			if (isGridKitClasses == null){
				isGridKitClasses = isGridKitClassesImpl();
			}
			return isGridKitClasses;
		}

		public synchronized boolean isTestClasses() {
			if (isTestClasses == null){
				isTestClasses = isTestClassesImpl();
			}
			return isTestClasses;
		}

		private boolean isGridKitClassesImpl() {
			try {
				if (getLocalFile().isFile()) {
					ZipFile zipFile = new ZipFile(getLocalFile());
					final ZipEntry gridKitEntry = zipFile.getEntry("org/gridkit");
					return gridKitEntry != null;
				} else if (getLocalFile().isDirectory()) {
					final File gridKitPackage = new File(new File(getLocalFile(), "org"), "gridkit");
					return gridKitPackage.exists();
				} else {
					final ZipInputStream zipInputStream = new ZipInputStream(getContent());
					ZipEntry entry;
					while ((entry = zipInputStream.getNextEntry()) != null) {
						if (entry.getName().startsWith("org/gridkit/")) {
							return true;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		private boolean isTestClassesImpl(){
			return getFileName().contains("test-classes");
		}
	}
	
	static class ByteBlob implements FileBlob {

		private String filename;
		private String hash;
		private byte[] data;
		
		public ByteBlob(String filename, byte[] data) {
			this.filename = filename;
			this.data = data;
			this.hash = StreamHelper.digest(data, "SHA-1");
		}

		@Override
		public File getLocalFile() {
			return null;
		}

		@Override
		public String getFileName() {
			return filename;
		}

		@Override
		public String getContentHash() {
			return hash;
		}

		@Override
		public InputStream getContent() {
			return new ByteArrayInputStream(data);
		}

		@Override
		public long size() {
			return data.length;
		}
	}	
}
