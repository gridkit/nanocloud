package org.gridkit.nanocloud.viengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.vicluster.CloudContext.Helper;

public class NodeConfigHelper {

    private static PassivePragmaHandler PASSIVE_PRAGMA = new PassivePragmaHandler();
    private static PassthroughPragmaHandler PASSTHROUGH_PRAGMA = new PassthroughPragmaHandler();

    public static void require(PragmaWriter writer, String phase, String key) {
        require(writer, phase, key, null);
    }

    public static void require(PragmaWriter writer, String phase, String key, String description) {
        String vkey = Pragma.BOOT_VALIDATOR + phase + ".require:" + key;
        writer.set(vkey, new PresenceCheck(key, description));
    }

    public static void addPrePhase(PragmaWriter writer, String phase, String subphase) {
        writer.set(Pragma.BOOT_PHASE_PRE + phase + "." + subphase, "");
    }

    public static void addPostPhase(PragmaWriter writer, String phase, String subphase) {
        writer.set(Pragma.BOOT_PHASE_POST + phase + "." + subphase, "");
    }

    public static void action(PragmaWriter writer, String phase, String actionKey, NodeAction action) {
        writer.set(Pragma.BOOT_ACTION + phase + "." + actionKey, action);
    }

    public static void actionLink(PragmaWriter writer, String phase, String actionKey, String linkTarget) {
        writer.link(Pragma.BOOT_ACTION + phase + "." + actionKey, linkTarget);
    }

    public static void passivePragma(PragmaWriter writer, String pragma) {
        pragmaHandler(writer, pragma, PASSIVE_PRAGMA);
    }

    public static void passthroughPragma(PragmaWriter writer, String pragma) {
        pragmaHandler(writer, pragma, PASSTHROUGH_PRAGMA);
    }

    public static void pragmaHandler(PragmaWriter writer, String pragma, PragmaHandler handler) {
        writer.set(Pragma.NODE_PRAGMA_HANDLER + pragma, handler);
    }

    public static void setDefault(PragmaWriter writer, String key, Object value) {
        writer.set(Pragma.DEFAULT + key, value);
    }

    public static void setLazyDefault(PragmaWriter writer, String key, LazyPragma lazy) {
        writer.setLazy(Pragma.DEFAULT + key, lazy);
    }

    public static <T> void cloudSingleton(PragmaWriter writer, String key, Class<T> type, Consumer<T> shutdownLambda) {
        SharedEntity<T> entry = new SharedEntity<T>(Helper.key(type), Helper.reflectionProvider(type, shutdownLambda));
        writer.setLazy(key, entry);
    }

    public static void addFinalizer(PragmaWriter writer, String key, final Runnable runnable) {
        addFinalizer(writer, key, new NodeAction() {

            @Override
            public void run(PragmaWriter context) throws ExecutionException {
                runnable.run();
            }
        });
    }

    public static void addFinalizer(PragmaWriter writer, String key, NodeAction action) {
        String fkey = Pragma.NODE_FINALIZER + key;
        int n = 1;
        while(writer.isPresent(fkey)) {
            fkey = Pragma.NODE_FINALIZER + key + (n++);
        }
        writer.set(fkey, action);
    }


    /**
     * Allows simple templates base on node name.
     *
     * <pre>
     *   ~[%s-xx] !(.*)
     *   ^\------/ \--/
     *   | \        \ regEx pattern to be run on <code>name</code> parameters, matching groups are used as parameters for template expression
     *   |  \
     *   \   \ template expression, using {@link String#format(String, Object...)} notation.
     *    \
     *     \ template marker
     * </pre>
     *
     * String not prefixed by ~ will be used verbatim.
     *
     */
    public static String transform(String pattern, String name) {
        if (pattern == null || !pattern.startsWith("~")) {
            return pattern;
        }
        int n = pattern.indexOf('!');
        if (n < 0) {
            throw new IllegalArgumentException("Invalid host extractor [" + pattern + "]");
        }
        String format = pattern.substring(1, n);
        Matcher m = Pattern.compile(pattern.substring(n + 1)).matcher(name);
        if (!m.matches()) {
            throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
        }
        else {
            Object[] groups = new Object[m.groupCount()];
            for(int i = 0; i != groups.length; ++i) {
                groups[i] = m.group(i + 1);
                try {
                    groups[i] = Long.parseLong((String)groups[i]);
                }
                catch(NumberFormatException e) {
                    // ignore
                }
            }
            try {
                return String.format(format, groups);
            }
            catch(IllegalArgumentException e) {
                throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
            }
        }
    }

    /**
     * Allow some syntax sugar in <code>path</code> expression.
     *
     * <pre>
     * <code>?optionalPath</code> - will no throw if no file is available, will retunr null instead
     * </pre>
     *
     * <pre>
     * <code>~/ssh-credentials.prop</code> - "~" is resolved to home directory
     * </pre>
     * <pre>
     * <code>my.prop|~/ssh-credentials.prop</code> - few alternative paths could be specified using "|" as separator, first available will be used
     * </pre>
     */
    public static InputStream openStream(String path) throws IOException {
        if (path.startsWith("?")) {
            try {
                return openStream(path.substring(1));
            } catch (IOException e) {
                return null;
            }
        }
        String[] alts = path.split("[|]");
        for(String alt: alts) {
            try {
                InputStream is = openStreamSingle(alt);
                return is;
            } catch (IOException e) {
                // continue
            }
        }
        throw new FileNotFoundException("Path spec [" + path + "] was not resolved");
    }

    private static InputStream openStreamSingle(String path) throws IOException {
        InputStream is = null;
        if (path.startsWith("~/")) {
            String userHome = System.getProperty("user.home");
            File cpath = new File(new File(userHome), path.substring(2));
            is = new FileInputStream(cpath);
        }
        else if (path.startsWith("resource:")) {
            String rpath = path.substring("resource:".length());
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            is = cl.getResourceAsStream(rpath);
            if (is == null) {
                throw new FileNotFoundException("Resource not found '" + path + "'");
            }
        }
        else {
            if (new File(path).exists()) {
                is = new FileInputStream(new File(path));
            }
            else {
                try {
                    is = new URL(path).openStream();
                }
                catch(IOException e) {
                    // ignore
                }
                if (is == null) {
                    throw new FileNotFoundException("Cannot resolve path '" + path + "'");
                }
            }
        }
        return is;
    }

    static class PresenceCheck implements NodeAction {

        private String key;
        private String description;

        public PresenceCheck(String key, String description) {
            this.key = key;
            this.description = description;
        }

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            if (context.get(key) == null) {
                BootAnnotation.fatal((String)context.get(Pragma.BOOT_PHASE), "Missing required key '" + key + "'" + (description == null ? "" : " - " + description))
                    .append(context);
            }
        }

        @Override
        public String toString() {
            return "PresenceCheck[" + key + "]";
        }
    }

    public static NodeAction nodeActionFrom(final Runnable task) {
        return new NodeAction() {

            @Override
            public void run(PragmaWriter context) throws ExecutionException {
                task.run();
            }
        };
    }

    public static void addPreShutdownHook(PragmaWriter context, String name, NodeAction action) {
        context.set(Pragma.NODE_PRE_SHUTDOWN_HOOK + name, action);
    }

    public static void addShutdownHook(PragmaWriter context, String name, NodeAction action) {
        context.set(Pragma.NODE_SHUTDOWN_HOOK + name, action);
    }

    public static void addPostShutdownHook(PragmaWriter context, String name, NodeAction action) {
        context.set(Pragma.NODE_POST_SHUTDOWN_HOOK + name, action);
    }
}
