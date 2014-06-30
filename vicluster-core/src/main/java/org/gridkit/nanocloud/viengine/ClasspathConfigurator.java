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

import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;

public class ClasspathConfigurator implements NodeAction {

    public static final ClasspathConfigurator INSTANCE = new ClasspathConfigurator();
    
    @Override
    public void run(PragmaWriter context) throws ExecutionException {
        context.set(Pragma.RUNTIME_CLASSPATH, buildClasspath(context));
    }

    protected List<ClasspathEntry> buildClasspath(PragmaReader context) {
        try {
            List<String> tweaks = context.match(ClasspathConf.CLASSPATH_TWEAK + "**");
            List<ClasspathEntry> cp = Classpath.getClasspath(Thread.currentThread().getContextClassLoader());
            if (tweaks.isEmpty()) {
                return cp;
            } else {
                List<ClasspathEntry> entries = new ArrayList<Classpath.ClasspathEntry>(cp);

                for (String k : tweaks) {
                    String change = context.get(k);
                    if (change.startsWith("+")) {
                        String cpe = normalize(toURL(change.substring(1)));
                        addEntry(entries, cpe);
                    } else if (change.startsWith("-")) {
                        String cpe = normalize(toURL(change.substring(1)));
                        removeEntry(entries, cpe);
                    }
                    else {
                        throw new IllegalArgumentException("Cannot parse classpath tweak: " + tweaks);
                    }
                }

                return entries;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addEntry(List<ClasspathEntry> entries, String path) throws IOException {
        ClasspathEntry entry = Classpath.getLocalEntry(path);
        if (entry != null) {
            entries.add(0, entry);
        }
    }

    private void removeEntry(List<ClasspathEntry> entries, String path) {
        Iterator<ClasspathEntry> it = entries.iterator();
        while (it.hasNext()) {
            if (path.equals(normalize(it.next().getUrl()))) {
                it.remove();
            }
        }
    }

    private URL toURL(String path) {
        try {
            return new URL(path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String normalize(String path) {
        try {
            // normalize path entry if possible
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }

    private String normalize(URL url) {
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
