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

import java.io.BufferedOutputStream;
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
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ClasspathUtils {

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
		ZipOutputStream jarOut = manifest == null ? new JarOutputStream(bos) : new ZipOutputStream(bos);
		ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
		e.setTime(0l); // this to ensure equal hash for equal content
		jarOut.putNextEntry(e);
		manifest.write(new BufferedOutputStream(jarOut));
		jarOut.closeEntry();
		jarOut.close();
		byte[] jarFile = bos.toByteArray();
		return jarFile;
	}

	// unused
	public static byte[] createBootstrapperJar(Manifest manifest, Class<?> bootstrapper) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		String path = bootstrapper.getName().replace('.', '/') + ".class";
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
		int size = addFiles(jarOut, "", new File(path));
		if (size == 0) {
			// no files in folder
			return null;
		}
		jarOut.close();
		return bos.toByteArray();
	}

	private static int addFiles(JarOutputStream jarOut, String base, File path) throws IOException {
		int count = 0;
		for(File file : path.listFiles()) {
			if (file.isDirectory()) {
				count += addFiles(jarOut, base + file.getName() + "/", file);
			}
			else {
				JarEntry entry = new JarEntry(base + file.getName());
				entry.setTime(file.lastModified());
				jarOut.putNextEntry(entry);
				StreamHelper.copy(new FileInputStream(file), jarOut);
				jarOut.closeEntry();
				++count; 
			}
		}
		return count;
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
}
