package org.gridkit.vicluster.telecontrol;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class ClasspathUtilsTest {

    @Test
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

    @Test
    public void testJarFiles_withSpring() throws Exception {
        Random random = new Random();
        final String packageName = "package" + random.nextLong();
        final String classname = packageName + ".Test";

        final Path baseDir = Files.createTempDirectory("testJarFiles");

        final ClassPool pool = ClassPool.getDefault();
        final CtClass cc = pool.makeClass(classname);
        final ClassFile classFile = cc.getClassFile();
        ConstPool cp = classFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
        attr.setAnnotation(new Annotation("org.springframework.stereotype.Component", cp));
        classFile.addAttribute(attr);
        cc.writeFile(baseDir.toString());


        final byte[] jarAsBytes = ClasspathUtils.jarFiles(baseDir.toString());
        final File testJar = new File(baseDir.toFile(), "test.jar");
        FileOutputStream fos = new FileOutputStream(testJar);
        fos.write(jarAsBytes);
        fos.close();

        final URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
        final Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(classLoader, testJar.toURI().toURL());

        ApplicationContext context = new AnnotationConfigApplicationContext(packageName);
        final Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(Component.class);
        assertEquals(1, beansWithAnnotation.size());
        assertTrue(beansWithAnnotation.containsKey("test"));
        assertEquals(classname, beansWithAnnotation.get("test").getClass().getName());
    }
}