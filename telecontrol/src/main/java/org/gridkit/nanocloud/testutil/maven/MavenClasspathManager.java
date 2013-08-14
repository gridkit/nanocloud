/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.nanocloud.testutil.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility class to configure inclusion, exclusion and replacement
 * of maven dependencies in classpath.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class MavenClasspathManager {

	private static Class<?> ANCHOR = MavenClasspathManager.class;
	
	private static Map<String, SourceInfo> CLASSPATH_JARS;
	
	private static File LOCAL_MAVEN_REPOPATH;
	
	public static File getLocalMavenRepoPath() {
		initClasspath();
		findLocalMavenRepo();
		return LOCAL_MAVEN_REPOPATH;
	}

	/**
	 * This method analyzes JVM classpath and try to deduce classpath version
	 * of Maven dependency.
	 */
	public static String getArtifactVersion(String groupId, String artifactId) {
		String cppath = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
		InputStream is = ANCHOR.getResourceAsStream(cppath);
		if (is != null) {
			try {
				Properties prop = new Properties();
				prop.load(is);
				is.close();
				return prop.getProperty("version");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			// artifact we are looking for does not provide Maven manifest, let's take hard way
			initClasspath();
			for(SourceInfo si: CLASSPATH_JARS.values()) {
				if (si.mavenProps != null) {
					if (groupId.equals(si.mavenProps.get("groupId")) && artifactId.equals(si.mavenProps.get("artifactId"))) {
						return si.mavenProps.getProperty("version");
					}
				}
				if (si.jarUrl != null) {
					String artBase = getMavenArtifactBase(si.jarUrl);
					if (artBase != null) {
						String path = "/" + groupId.replace('.', '/') + "/" + artifactId;
						if (artBase.endsWith(path)) {
							return getMavenVersionFromRepoPath(si.jarUrl);
						}
					}
				}				
			}
			throw new IllegalArgumentException("Cannot detect version for " + groupId + ":" + artifactId);
		}
	}

	/**
	 * Detects local Maven repo (using JVM classpath) and enumerates
	 * all available versions for artifact. 
	 */
	public static List<String> getAvailableVersions(String groupId, String artifactId) {
		File localRepo = getLocalMavenRepoPath();
		if (localRepo == null) {
			throw new IllegalArgumentException("Cannot detect local repo");
		}
		String[] gp = groupId.split("[.]");
		File path = localRepo;
		for(String p: gp) {
			path = new File(path, p);
		}
		path = new File(path, artifactId);
		List<String> version = new ArrayList<String>();
		for(File c: path.listFiles()) {
			if (c.isDirectory()) {
				if (findJar(c, artifactId) != null) {
					version.add(c.getName());
				}
			}
		}
		Collections.sort(version);
		return version;
	}
	
	/**
	 * Do {@link #removeArtifactVersion(ViConfigurable, String, String)} then {@link #addArtifactVersion(ViConfigurable, String, String, String)}. 
	 */
	public static void replaceArtifactVersion(ViConfigurable node, String groupId, String artifactId, String version) {
		removeArtifactVersion(node, groupId, artifactId);
		addArtifactVersion(node, groupId, artifactId, version);
	}

	/**
	 * Detects classpath element for corresponding Maven artifact in current JVM classpath
	 * and exlude it from {@link ViNode}'s classpath. 
	 */
	public static void removeArtifactVersion(ViConfigurable node, String groupId, String artifactId) {
		String version = getArtifactVersion(groupId, artifactId);
		if (version == null) {
			throw new IllegalArgumentException("Artifact " + groupId + ":" + artifactId + " wasn't detected in classpath");
		}
		URL url = getArtifactClasspathUrl(groupId, artifactId);
		if (url == null) {
			throw new IllegalArgumentException("Artifact " + groupId + ":" + artifactId + " wasn't detected in classpath");
		}
		if ("jar".equals(url.getProtocol())) {
			url = baseUrlToJarUrl(url);
		}
		if (url == null) {
			dumpClasspathInfo();
			throw new IllegalArgumentException("Artifact " + groupId + ":" + artifactId + " wasn't detected in classpath");
		}
		
		try {
			if ("file".equals(url.getProtocol())) {
				File file = new File(url.toURI());
				if (!file.exists()) {
					throw new IllegalArgumentException("Artifact " + groupId + ":" + artifactId + ":" + version + " is not available");
				}
				node.setProp(JvmProps.CP_REMOVE + file.getPath(), file.getPath());
			}
			else {
				throw new IllegalArgumentException("Bad URL " + url.toString());
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Bad URL " + url.toString());
		}
	}

	/**
	 * Resolve path to specific version of Maven artifact and excludes it from {@link ViNode}'s classpath.
	 * Useful when artifact is not a part of current JVM classpath.  
	 */
	public static void removeArtifactVersion(ViConfigurable node, String groupId, String artifactId, String version) {
		URL url = findJar(groupId, artifactId, version);
		
		if (url == null) {
			throw new IllegalArgumentException("Cannot locate artifact: " + groupId + ":" + artifactId + ":" + version);
		}
		
		try {
			if ("file".equals(url.getProtocol())) {
				File file = new File(url.toURI());
				if (!file.exists()) {
					throw new IllegalArgumentException("Artifact " + groupId + ":" + artifactId + ":" + version + " is not available");
				}
				node.setProp(JvmProps.CP_REMOVE + file.getPath(), file.getPath());
			}
			else {
				throw new IllegalArgumentException("Bad URL " + url.toString());
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Bad URL " + url.toString());
		}
	}

	/**
	 * Resolves path for Maven artifact with specific version and adds it to classpath.
	 */
	public static void addArtifactVersion(ViConfigurable node, String groupId, String artifactId, String version) {
		URL url = findJar(groupId, artifactId, version);
		if (url == null) {
			throw new IllegalArgumentException("Artifact " + groupId + ":" + artifactId + ":" + version + " is not available");
		}
		try {
			if ("file".equals(url.getProtocol())) {
				File file = new File(url.toURI());
				if (!file.exists()) {
					throw new IllegalArgumentException("Artifact " + groupId + ":" + artifactId + ":" + version + " is not available");
				}
				node.setProp(JvmProps.CP_ADD + file.getPath(), file.getPath());
			}
			else {
				throw new IllegalArgumentException("Bad URL " + url.toString());
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Bad URL " + url.toString());
		}
	}
	
	private static String findJar(File c, String artifactId) {
		File[] files = c.listFiles();
		if (files != null) {
			for(File f: files) {			
				if (!f.isDirectory() && f.getName().startsWith(artifactId + "-") && f.getName().endsWith(".jar")) {
					// TODO read maven metadata xml
					if (!f.getName().endsWith("-javadoc.jar") && !f.getName().endsWith("-sources.jar")) {
						return f.getName();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Detects and returns root URL of specific Maven artifact for current JVM classpath.
	 */
	public static URL getArtifactClasspathUrl(String groupId, String artifactId) {
		String cppath = "/META-INF/maven/" + groupId + "/" + artifactId;
		URL url = ANCHOR.getResource(cppath);
		if (url != null) {
			try {
				String us = url.toExternalForm();
				us = us.substring(0, us.length() - cppath.length() + 1);
				return new URL(us);
			} catch (IOException e) {
				throw new RuntimeException("Failed to derive root path from resouce URL '" + url.toExternalForm() + "', resource '"+ cppath + "'", e);
			}
		}
		else {
			// artifact we are looking for does not provide Maven manifest, let's take hard way
			initClasspath();
			for(SourceInfo si: CLASSPATH_JARS.values()) {
				if (si.matchMavenProps(groupId, artifactId)) {
					try {
						return new URL(si.baseUrl);
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				}
			}
			
			for(SourceInfo si: CLASSPATH_JARS.values()) {
				if (si.jarUrl != null) {
					String artBase = getMavenArtifactBase(si.jarUrl);
					if (artBase != null) {
						String path = "/" + groupId.replace('.', '/') + "/" + artifactId;
						if (artBase.endsWith(path)) {
							try {
								return new URL(si.baseUrl);
							} catch (MalformedURLException e) {
								throw new RuntimeException(e);
							}
						}
					}
				}
			}
			throw new IllegalArgumentException("Cannot detect version for " + groupId + ":" + artifactId);
		}
	}

	/**
	 * Return jar path URL for specific artifact from detected Maven local repo.
	 */
	public static URL findJar(String groupId, String artifactId, String version) {
		File localRepo = getLocalMavenRepoPath();
		// TODO search in local classpath first, jar may be in reactor
		if (localRepo == null) {
			throw new IllegalArgumentException("Cannot detect local repo");
		}
		String[] gp = groupId.split("[.]");
		File path = localRepo;
		for(String p: gp) {
			path = new File(path, p);
		}
		path = new File(path, artifactId);
		path = new File(path, version);
		String jarName = findJar(path, artifactId);
		if (jarName != null) {
			File jar = new File(path, jarName);
			try {
				return jar.toURI().toURL();
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Dumps detected Maven artifact coordinates for current JVM classpath.
	 * Useful for debuging.
	 */
	public static void dumpClasspathInfo() {
		initClasspath();
		for(SourceInfo si: CLASSPATH_JARS.values()) {
			if (si.mavenProps == null) {
				System.out.println("<unknown> " + si.baseUrl);
			}
			else {
				String g = si.mavenProps.getProperty("groupId");
				String a = si.mavenProps.getProperty("artifactId");
				String v = si.mavenProps.getProperty("version");
				System.out.println(g + ":" + a + ":" + v + " " + si.baseUrl);
			}
		}
	}
	
	private static URL baseUrlToJarUrl(URL url) {
		if ("jar".equals(url.getProtocol())) {
			String u = url.toExternalForm();
			u = u.substring("jar:".length());
			int c = u.lastIndexOf('!');
			if (c < 0) {
				throw new IllegalArgumentException(url.toExternalForm() + " doesn't ends with !/");
			}
			u = u.substring(0, c);
			try {
				return new URL(u);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			throw new IllegalArgumentException("Protocol is to jar: " + url.toExternalForm());
		}
	}
	
	private static String getMavenArtifactBase(String path) {
		int c = path.lastIndexOf('/');
		if (c <= 0) {
			return null;
		}
		path = path.substring(0, c);
		c = path.lastIndexOf('/');
		if (c <= 0) {
			return null;
		}
		path = path.substring(0, c);
		return path;
	}

	private static String getMavenVersionFromRepoPath(String path) {
		int c = path.lastIndexOf('/');
		if (c <= 0) {
			return null;
		}
		path = path.substring(0, c);
		c = path.lastIndexOf('/');
		if (c <= 0) {
			return null;
		}
		path = path.substring(c + 1);
		return path;
	}
	
	private synchronized static void initClasspath() {
		if (CLASSPATH_JARS != null) {
			return;
		}
		CLASSPATH_JARS = new HashMap<String, MavenClasspathManager.SourceInfo>();
		for(URL url: getClasspathResources("META-INF/MANIFEST.MF")) {
			SourceInfo info;
			try {
				String upath = url.toString();
				upath = upath.substring(0, upath.length() - "META-INF/MANIFEST.MF".length());
				info = readInfo(upath);
				CLASSPATH_JARS.put(info.baseUrl, info);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for(URL url: getClasspathResources("")) {
			SourceInfo info;
			try {
				info = readInfo(url.toString());
				CLASSPATH_JARS.put(info.baseUrl, info);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static List<URL> getClasspathResources(String path) {
		Enumeration<URL> en;
		try {
			en = ANCHOR.getClassLoader().getResources(path);
		} catch (IOException e) {
			throw new RuntimeException("Failed to scan classpath", e);
		}
		List<URL> list = new ArrayList<URL>();
		while(en.hasMoreElements()) {
			list.add(en.nextElement());
		}
		return list;
	}
	
	private synchronized static void findLocalMavenRepo() {
		initClasspath();
		if (LOCAL_MAVEN_REPOPATH != null) {
			return;
		}
		for(SourceInfo si:	CLASSPATH_JARS.values()) {
			try {
				if (si.mavenProps != null && si.jarUrl != null) {
					String group = si.mavenProps.getProperty("groupId");
					String artifact = si.mavenProps.getProperty("artifactId");
					String version = si.mavenProps.getProperty("version");
					String path = "/" + group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
					if (si.jarUrl.endsWith(path)) {
						String repo = si.jarUrl.substring(0, si.jarUrl.length() - path.length());
						URI ru = new URI(repo);
						LOCAL_MAVEN_REPOPATH = new File(ru);
						return;
					}
				}
			} catch (URISyntaxException e) {
				// ignore
			}
		}
	}
	
	private static SourceInfo readInfo(String upath) throws IOException {
		SourceInfo info = new SourceInfo();
		try {
			URL url = new URL(upath + "META-INF/MANIFEST.MF");
			InputStream is = url.openStream();
			if (is != null) {
				Manifest mf = new Manifest();
				mf.read(is);
				is.close();
				info.manifest = mf;
			}
		}
		catch(IOException e) {
			// ignore
		}
		if (upath.startsWith("jar:")) {
			String jarPath = upath.substring("jar:".length());
			int c = jarPath.indexOf('!');
			jarPath = jarPath.substring(0, c);
			info.jarUrl = jarPath;
		}
		info.baseUrl = upath;
		info.mavenProps = loadMavenProps(upath);
		return info;
	}

	private static Properties loadMavenProps(String upath) throws IOException {
		List<String> paths = listFiles(new ArrayList<String>(), new URL(upath + "META-INF/maven/"), "META-INF/maven/");
		Properties prop = null;
		for(String path: paths) {
			if (path.endsWith("/pom.properties")) {
				if (prop != null) {
					// ambiguous maven properties, ignoring
					return null;
				}
				URL url = new URL(upath + path);
				InputStream is = url.openStream();
				prop = new Properties();
				prop.load(is);
				is.close();
			}
		}
		if (prop == null && upath.startsWith("file:")) {
			// unfortunately vanilla maven test run will not have pom.properties on classpath
			// in this case lets check presence of pom.xml
			if (!upath.endsWith("test-classes/")) {
				// we should ignore test classes
				// current test classpath should be available otherwise retoning will not work
				try {
					File f = new File(new URI(upath));
					File pom = new File(f.getParentFile().getParentFile(), "pom.xml");
					if (pom.isFile()) {
						prop = loadPomProp(pom);
					}
				} catch (Exception e) {
					// ignore error
				}
			}
		}
		return prop;
	}

	private static Properties loadPomProp(File pom) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document doc = builder.parse(pom);
		Element root = doc.getDocumentElement();
		
		String groupId = getText(root, "groupId");
		String artifactId = getText(root, "artifactId");
		String version = getText(root, "version");
		
		if (groupId == null || groupId.startsWith("$")) {
			groupId = getText(root, "parent/groupId");
		}
		if (version == null || version.startsWith("$")) {
			version = getText(root, "parent/version");
		}

		if (	   groupId != null && !groupId.startsWith("$") 
				&& artifactId != null && !artifactId.startsWith("$")
				&& version != null && !version.startsWith("$")) {
			
			Properties props = new Properties();
			props.put("groupId", groupId);
			props.put("artifactId", artifactId);
			props.put("version", version);
			return props;
		}
		else {
			// TODO warning
			return null;
		}
	}

	private static String getText(Element e, String path) {
		String[] tags = path.split("[/]");
		path:
		for(String tag: tags) {
			NodeList nl = e.getChildNodes();
			for(int i = 0; i != nl.getLength(); ++i) {
				Node n = nl.item(i);
				if (n instanceof Element && tag.equals(((Element)n).getNodeName())) {
					e = (Element) n;
					continue path;
				}
			}
			return null;
		}
		return e.getTextContent();
	}
	
	private static class SourceInfo {
		
		String baseUrl;
		String jarUrl;
		@SuppressWarnings("unused")
		Manifest manifest;
		Properties mavenProps;
		
		public boolean matchMavenProps(String groupId, String artifactId) {
			if (mavenProps == null) {
				return false;
			}
			else {
				return groupId.equals(mavenProps.get("groupId")) && artifactId.equals(mavenProps.get("artifactId"));
			}
		}
	}	
	
	static List<String> findFiles(String path) throws IOException {
		List<String> result = new ArrayList<String>();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> en = cl.getResources(path);
		while(en.hasMoreElements()) {
			URL u = en.nextElement();
			listFiles(result, u, path);
		}
		return result;
	}
	
	static List<String> listFiles(List<String> results, URL packageURL, String path) throws IOException {

	    if(packageURL.getProtocol().equals("jar")){
	        String jarFileName;
	        JarFile jf ;
	        Enumeration<JarEntry> jarEntries;
	        String entryName;

	        // build jar file name, then loop through zipped entries
	        jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
	        jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
	        jf = new JarFile(jarFileName);
	        jarEntries = jf.entries();
	        while(jarEntries.hasMoreElements()){
	            entryName = jarEntries.nextElement().getName();
	            if(entryName.startsWith(path)){
	                results.add(entryName);
	            }
	        }

	    // loop through files in classpath
	    }else{
	        File dir = new File(packageURL.getFile());
	        String cp = dir.getCanonicalPath();
	        File root = dir;
	        while(true) {
	        	if (cp.equals(new File(root, path).getCanonicalPath())) {
	        		break;
	        	}
	        	root = root.getParentFile();
	        }
	        listFiles(results, root, dir);
	    }
	    return results;
	}

	static void listFiles(List<String> names, File root, File dir) {
		String rootPath = root.getAbsolutePath(); 
		if (dir.exists() && dir.isDirectory()) {
			String dname = dir.getAbsolutePath().substring(rootPath.length() + 1);
			dname = dname.replace('\\', '/');
			names.add(dname);
			for(File file: dir.listFiles()) {
				if (file.isDirectory()) {
					listFiles(names, root, file);
				}
				else {
					String name = file.getAbsolutePath().substring(rootPath.length() + 1);
					name = name.replace('\\', '/');
					names.add(name);
				}
			}
		}
	}	
}
