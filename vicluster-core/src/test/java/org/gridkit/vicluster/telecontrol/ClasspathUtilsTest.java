package org.gridkit.vicluster.telecontrol;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClasspathUtilsTest extends TestCase {

    public void testJarFiles() throws Exception {
        final Path baseDir = Files.createTempDirectory("testJarFiles");
        Files.createDirectory(Paths.get(baseDir.toString(), "1"));
        Files.createFile(Paths.get(baseDir.toString(), "1", "test.txt"));
        final byte[] jarAsBytes = ClasspathUtils.jarFiles(baseDir.toString());
        final File testJar = new File(baseDir.toFile(), "test.jar");
        FileOutputStream fos = new FileOutputStream(testJar);
        fos.write(jarAsBytes);
        fos.close();
        URLClassLoader urlClassLoader = null;
        try{
            urlClassLoader = new URLClassLoader(new URL[]{testJar.toURI().toURL()});
            final URL resource = urlClassLoader.getResource("1");
            assertNotNull(resource);
        }finally {
            if (urlClassLoader!=null){
                urlClassLoader.close();
            }
        }
    }
}