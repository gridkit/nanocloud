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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * TODO make it package private
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ClasspathUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathUtils.class);

    private static final ConcurrentMap<String, String> MISSING_URL = new ConcurrentHashMap<String, String>(64, 0.75f, 1);

    public static List<String> getStartupClasspath() {
        return sanitize(System.getProperty("java.class.path"));
    }

    private static Class urlClassPath;

    private static Method urlClassPathGetUrlsField;

    private static Class builtinClassLoader;

    private static Field builtinClassLoaderUcpField;

    static {
        try {
            builtinClassLoader = Class.forName("jdk.internal.loader.BuiltinClassLoader");
            builtinClassLoaderUcpField = builtinClassLoader.getDeclaredField("ucp");
        } catch (Exception e) {
            builtinClassLoader = null;
            builtinClassLoaderUcpField = null;
        }
        try {
            urlClassPath = Class.forName("jdk.internal.loader.URLClassPath");
            urlClassPathGetUrlsField = urlClassPath.getMethod("getURLs");
        } catch (Exception e) {
            urlClassPath = null;
            urlClassPathGetUrlsField = null;
        }
    }

    private static List<String> sanitize(String cp) {
        List<String> scp = new ArrayList<String>();
        String psep = System.getProperty("path.separator");
        for(String path: cp.split(psep)) {
            try {
                String spath = new File(path).getAbsoluteFile().getCanonicalPath();
                scp.add(spath);
            }
            catch(IOException e) {
                // ignore erroneous classpath element
            }
        }
        return scp;
    }

    public static Collection<URL> listCurrentClasspath() {
        URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        return listCurrentClasspath(classLoader);
    }

    public static ClassLoader getNearestSystemClassloader(ClassLoader cl) {
        while(cl != null) {
            if (cl instanceof URLClassLoader) {
                if (cl.getClass().getName().endsWith("$ExtClassLoader")) {
                    return cl;
                }
            } else {
                // post Java 8 non-sense
                if (cl.getClass().getName().equals("jdk.internal.loader.ClassLoaders$PlatformClassLoader")) {
                    return cl;
                }
            }
            cl = cl.getParent();
        }

        return null;
    }

    public static Collection<URL> listCurrentClasspath(ClassLoader classLoader) {
        Set<URL> result = new LinkedHashSet<URL>();
        while(true) {
            for(URL url: getUrlClasspath(classLoader)) {
                addEntriesFromManifest(result, url);
            }
            ClassLoader cls = classLoader.getParent();
            if (cls instanceof URLClassLoader) {
                if (cls.getClass().getName().endsWith("$ExtClassLoader")) {
                    break;
                }
                classLoader = (URLClassLoader) cls;
            }
            else {
                break;
            }
        }
        return new ArrayList<URL>(result);
    }

    private static URL[] getUrlClasspath(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        } else {
            if (builtinClassLoader != null && builtinClassLoaderUcpField != null
                    && builtinClassLoader.isAssignableFrom(classLoader.getClass())) {
                return getJava9ClassloaderUrls(classLoader);
            }
        }
        throw new IllegalArgumentException("Classloader classpath is not available " + classLoader.getClass().getName(), null);
    }

    private static URL[] getJava9ClassloaderUrls(ClassLoader classLoader) {
        try {
            synchronized (builtinClassLoaderUcpField) {
                boolean accessible = builtinClassLoaderUcpField.isAccessible();
                try {
                    if (!accessible) {
                        builtinClassLoaderUcpField.setAccessible(true);
                    }
                    Object ucpValue = builtinClassLoaderUcpField.get(classLoader);
                    if (ucpValue instanceof URLClassLoader) {
                        return ((URLClassLoader) ucpValue).getURLs();
                    } else {
                        if (urlClassPath != null && urlClassPath.isInstance(ucpValue)) {
                            return (URL[]) urlClassPathGetUrlsField.invoke(ucpValue);
                        } else {
                            throw new IllegalArgumentException("Classloader classpath is not available " + classLoader.getClass().getName());
                        }
                    }
                } finally {
                    if (!accessible) {
                        builtinClassLoaderUcpField.setAccessible(false);
                    }
                }
            }
        } catch (InvocationTargetException ex) {
            throw new IllegalArgumentException("Classloader classpath is not available " + classLoader.getClass().getName(), ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException("Classloader classpath is not available " + classLoader.getClass().getName(), ex);
        } catch (RuntimeException ex) {
            // This exception is not available in Java 6
            if ("InaccessibleObjectException".equals(ex.getClass().getSimpleName())) {
                throw new IllegalArgumentException("Classloader classpath cannot be accessed. Add following option to JVM arguments --add-opens java.base/jdk.internal.loader=ALL-UNNAMED " + classLoader.getClass().getName(), ex);
            } else {
                throw ex;
            }
        }
    }

    private static void addEntriesFromManifest(Set<URL> list, URL url) {
        if (list.contains(url)) {
            // avoid duplicates in list
            return;
        }
        // TODO eliminate manifest only classpaths
        try {
            InputStream is;
            try {
                is = url.openStream();
            }
            catch(Exception e) {
                String path = url.toString();
                if (MISSING_URL.put(path, path) == null) {
                    LOGGER.warn("URL not found and will be excluded from classpath: " + path);
                }
                throw e;
            }
            if (is != null) {
                list.add(url);
            }
            else {
                String path = url.toString();
                if (MISSING_URL.put(path, path) == null) {
                    LOGGER.warn("URL not found and will be excluded from classpath: " + path);
                }
            }
            JarInputStream jar = new JarInputStream(is);
            Manifest mf = jar.getManifest();
            jar.close();
            if (mf == null) {
                return;
            }
            String cp = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (cp != null) {
                for(String entry: cp.split("\\s+")) {
                    try {
                        URL ipath = new URL(url, entry);
                        addEntriesFromManifest(list, ipath);
                    }
                    catch(Exception e) {
                        // ignore
                    }
                }
            }
        }
        catch(Exception e) {
        }
    }

    public static byte[] createManifestJar(Manifest manifest) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream jarOut = manifest == null ? new JarOutputStream(bos) : new ZipOutputStream(bos);
        ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
        e.setTime(0l); // this to ensure equal hash for equal content
        jarOut.putNextEntry(e);
        manifest.write(jarOut);
        jarOut.closeEntry();
        jarOut.close();
        byte[] jarFile = bos.toByteArray();
        return jarFile;
    }

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
        ZipOutputStream jarOut = new ZipOutputStream(bos);
        if (manifest != null) {
            ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
            e.setTime(0l); // this to ensure equal hash for equal content
            jarOut.putNextEntry(e);
            manifest.write(jarOut);
            jarOut.closeEntry();
        }
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
                final String dirName = base + file.getName() + "/";

                JarEntry entry = new JarEntry(dirName);
                entry.setTime(0l);// this to ensure equal hash for equal content
                jarOut.putNextEntry(entry);
                jarOut.closeEntry();
                count += addFiles(jarOut, dirName, file);
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

    private static void addFiles(ZipOutputStream jarOut, String basePackage, String baseUrl) throws IOException, MalformedURLException {
        if (baseUrl.startsWith("jar:")) {
            int n = baseUrl.lastIndexOf("!");
            if (n < 0) {
                throw new IllegalArgumentException("Unexpected classpath URL: " + baseUrl);
            }
            String fileUrl = baseUrl.substring(4, n);
            InputStream is = new URL(fileUrl).openStream();
            ZipInputStream zis = new ZipInputStream(is);
            while(true) {
                ZipEntry ze = zis.getNextEntry();
                if (ze != null) {
                    if (ze.getName().startsWith(basePackage)) {
                        ZipEntry entry = new ZipEntry(ze.getName());
                        entry.setTime(0); // this is to facilitate content cache
                        jarOut.putNextEntry(entry);
                        StreamHelper.copyNoClose(zis, jarOut);
                        jarOut.closeEntry();
                    }
                    zis.closeEntry();
                }
                else {
                    break;
                }
            }
        }
        else {
            String urlBase = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
            InputStream is = new URL(urlBase).openStream();
            for(String line: StreamHelper.toLines(is)) {
                String fpath = urlBase + "/" + line;
                String jpath = basePackage + "/" + line;
                ZipEntry entry = new ZipEntry(jpath);
                entry.setTime(0); // this is to facilitate content cache
                jarOut.putNextEntry(entry);
                StreamHelper.copy(new URL(fpath).openStream(), jarOut);
                jarOut.closeEntry();
            }
        }
    }
}
