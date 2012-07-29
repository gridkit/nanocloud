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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.gridkit.vicluster.telecontrol.bootstraper.Bootstraper;

public class ClasspathUtils {

	public static Collection<URL> listCurrentClasspath() {
		URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		return Arrays.asList(classLoader.getURLs());
	}

//	public static Collection<URL> listCurrentEffectiveClasspath() throws IOException {
//		List<URL> result = new ArrayList<URL>();
//		Enumeration<URL> en = Thread.currentThread().getContextClassLoader().getResources("/");
//		while(en.hasMoreElements()) {
//			result.add(en.nextElement());
//		}
//		return result;
//	}
	
	public static byte[] createManifestJar(Manifest manifest) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
		JarOutputStream jarOut = manifest == null ? new JarOutputStream(bos) : new JarOutputStream(bos, manifest);
		jarOut.close();
		byte[] jarFile = bos.toByteArray();
		return jarFile;
	}

	public static byte[] createBootstraperJar(Manifest manifest) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		String path = Bootstraper.class.getName().replace('.', '/') + ".class";
		String basePackage = path.substring(0, path.lastIndexOf('/'));
		URL url = cl.getResource(path);
		String urlp = url.toExternalForm();
		if (urlp.indexOf('?') > 0) {
			urlp = urlp.substring(0, urlp.indexOf('?'));
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
		JarOutputStream jarOut = manifest == null ? new JarOutputStream(bos) : new JarOutputStream(bos, manifest);
		addFiles(jarOut, basePackage, urlp);
		jarOut.close();
		byte[] jarFile = bos.toByteArray();
		return jarFile;
	}
	
	public static byte[] jarFiles(String path) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		JarOutputStream jarOut = new JarOutputStream(bos);
		addFiles(jarOut, "", new File(path));
		jarOut.close();
		return bos.toByteArray();
	}

	private static void addFiles(JarOutputStream jarOut, String base, File path) throws IOException {
		for(File file : path.listFiles()) {
			if (file.isDirectory()) {
				addFiles(jarOut, base + file.getName() + "/", file);
			}
			else {
				JarEntry entry = new JarEntry(base + file.getName());
				entry.setTime(file.lastModified());
				jarOut.putNextEntry(entry);
				StreamHelper.copy(new FileInputStream(file), jarOut);
				jarOut.closeEntry();
			}
		}
	}

	private static void addFiles(JarOutputStream jarOut, String basePackage, String baseUrl) throws IOException, MalformedURLException {
		String urlBase = baseUrl.substring(0, baseUrl.lastIndexOf('/'));		
		InputStream is = new URL(urlBase).openStream();
		for(String line: StreamHelper.toLines(is)) {
			String fpath = urlBase + "/" + line;
			String jpath = basePackage + "/" + line;
			JarEntry entry = new JarEntry(jpath);
			entry.setTime(0); // this is to facilitate content cache			
			jarOut.putNextEntry(entry);
			StreamHelper.copy(new URL(fpath).openStream(), jarOut);
			jarOut.closeEntry();
		}		
	}
	
	public static void main(String[] args) throws MalformedURLException, IOException {
		byte[] jar = createBootstraperJar(null);
		System.out.println("Jar size: " + jar.length);
		for(URL url: listCurrentClasspath()) {
			System.out.println(url);
		}
		System.out.println();
//		for(URL url: listCurrentEffectiveClasspath()) {
//			System.out.println(url);
//		}
		
//		ClassLoader cl = Thread.currentThread().getContextClassLoader();
//		String path = Bootstraper.class.getName().replace('.', '/') + ".class";
//		URL url = cl.getResource(path);
//		String urlp = url.toExternalForm();
//		if (urlp.indexOf('?') > 0) {
//			urlp = urlp.substring(0, urlp.indexOf('?'));
//		}
//		String urlBase = urlp.substring(0, urlp.lastIndexOf('/'));		
//		InputStream is = new URL(urlBase).openStream();
//		System.out.println("Bootstrap: " + StreamHelper.toString(is));
	}
}
