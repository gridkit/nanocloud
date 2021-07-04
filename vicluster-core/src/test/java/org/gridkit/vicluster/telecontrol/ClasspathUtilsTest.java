package org.gridkit.vicluster.telecontrol;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Random;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.nanocloud.testutil.JvmVersionCheck;
import org.gridkit.vicluster.ViNode;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

public class ClasspathUtilsTest {

    @Test
    public void verify_jar_has_dir_entry() throws Exception {
        final File baseDir = new File("target/testJarFiles/" + System.currentTimeMillis());
        new File(baseDir, "1").mkdirs();
        new FileOutputStream(new File(baseDir, "test.txt")).close();
        final byte[] jarAsBytes = ClasspathUtils.jarFiles(baseDir.toString());
        final File testJar = new File(baseDir, "test.jar");
        FileOutputStream fos = new FileOutputStream(testJar);
        fos.write(jarAsBytes);
        fos.close();
        URLClassLoader urlClassLoader = null;
        urlClassLoader = new URLClassLoader(new URL[]{testJar.toURI().toURL()});
        final URL resource = urlClassLoader.getResource("1");
        assertNotNull(resource);
    }

    @Test
    public void verify_spring_can_find_files() throws Exception {

        // This test is broken for Java 11
        Assume.assumeTrue(JvmVersionCheck.isJava8orBelow());

        Random random = new Random();
        final String packageName = "package" + random.nextLong();
        final String classname = packageName + ".Test";

        final File baseDir = new File("target/testClasspathWithSpring/" + System.currentTimeMillis());
        baseDir.mkdirs();

        final ClassPool pool = ClassPool.getDefault();
        final CtClass cc = pool.makeClass(classname);
        final ClassFile classFile = cc.getClassFile();
        ConstPool cp = classFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
        attr.setAnnotation(new Annotation("org.springframework.stereotype.Component", cp));
        classFile.addAttribute(attr);
        cc.writeFile(baseDir.toString());


        final byte[] jarAsBytes = ClasspathUtils.jarFiles(baseDir.toString());
        final File testJar = new File(baseDir, "test.jar");
        FileOutputStream fos = new FileOutputStream(testJar);
        fos.write(jarAsBytes);
        fos.close();

        final URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
        final Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(classLoader, testJar.toURI().toURL());

        @SuppressWarnings("resource")
        ApplicationContext context = new AnnotationConfigApplicationContext(packageName);
        final Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(Component.class);
        assertEquals(1, beansWithAnnotation.size());
        assertTrue(beansWithAnnotation.containsKey("test"));
        assertEquals(classname, beansWithAnnotation.get("test").getClass().getName());
    }

    @Test
    public void verify_spring_data_jpa_can_be_fallbacked_to_default_package(){
        // if package does not exists, then spring-data-jpa fallback to default package.
        // See org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager.defaultPersistenceUnitRootLocation
        ViNode node = CloudFactory.createCloud().node("node");
        node.x(VX.TYPE).setLocal();
        node.x(VX.CLASSPATH).useShallowClasspath(false);
        node.exec(new Runnable() {
            @Override
            public void run() {
                JpaSpringApplication.main(new String[0]);
            }
        });
    }

    @EntityScan("com.unexisting.package")
    @SpringBootApplication
    public static class JpaSpringApplication{
        public static void main(String[] args) {
            SpringApplication.run(JpaSpringApplication.class);
        }
    }
}
