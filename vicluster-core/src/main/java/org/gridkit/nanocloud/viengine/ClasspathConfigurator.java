package org.gridkit.nanocloud.viengine;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;

public class ClasspathConfigurator implements NodeAction {

    public static final ClasspathConfigurator INSTANCE = new ClasspathConfigurator();
    
    @Override
    public void run(PragmaWriter context) throws ExecutionException {
        context.set(Pragma.RUNTIME_CLASSPATH, buildClasspath(context));
    }

    public static List<ClasspathEntry> buildClasspath(PragmaReader config) {
        try {
            List<String> tweaks = config.match(ViConf.CLASSPATH_TWEAK + "**");
            final boolean inheritClassPath = !Boolean.FALSE.toString().equalsIgnoreCase((String)config.get(ViConf.CLASSPATH_INHERIT));
            final List<ClasspathEntry> cp;
            final List<ClasspathEntry> inheritedClasspath = Classpath.getClasspath(Thread.currentThread().getContextClassLoader());
            if (inheritClassPath){
                cp = inheritedClasspath;
            }else {
                cp = new ArrayList<ClasspathEntry>();
                for (ClasspathEntry classpathEntry : inheritedClasspath) {
                    if (isGridKitClasses(classpathEntry) || isTestClasses(classpathEntry)){
                        cp.add(classpathEntry);
                    }
                }
            }
            if (tweaks.isEmpty()) {
                return cp;
            }
            else {
                List<ClasspathEntry> entries = new ArrayList<Classpath.ClasspathEntry>(cp);
                
                for(String k: tweaks) {
                    String change = config.get(k);
                    if (change.startsWith("+")) {
                        String cpe = normalize(toURL(change.substring(1)));
                        addEntry(entries, cpe);
                    }
                    else if (change.startsWith("-")) {
                        String cpe = normalize(toURL(change.substring(1)));
                        removeEntry(entries, cpe);
                    }
                }
                
                return entries;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isGridKitClasses(ClasspathEntry classpathEntry){
        try {
            final ZipInputStream zipInputStream = new ZipInputStream(classpathEntry.getContent());
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().startsWith("org/gridkit/")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isTestClasses(ClasspathEntry classpathEntry){
        return classpathEntry.getFileName().contains("test-classes");
    }

    private static void addEntry(List<ClasspathEntry> entries, String path) throws IOException {
        ClasspathEntry entry = Classpath.getLocalEntry(path);
        if (entry != null) {
            entries.add(0, entry);
        }
    }

    private static void removeEntry(List<ClasspathEntry> entries, String path) {
        Iterator<ClasspathEntry> it = entries.iterator();
        while(it.hasNext()) {
            if (path.equals(normalize(it.next().getUrl()))) {
                it.remove();
            }
        }
    }
            
    private static URL toURL(String path) {
        try {
            return new URL(path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String normalize(String path) {
        try {
            // normalize path entry if possible
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }
    
    private static String normalize(URL url) {
        try {
            if (!"file".equals(url.getProtocol())) {
                throw new IllegalArgumentException("Non file URL in classpath: " + url);
            }
            File f = new File(url.toURI());
            String path = f.getPath();
            return normalize(path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL in classpath: " + url);
        }
    }            
}
