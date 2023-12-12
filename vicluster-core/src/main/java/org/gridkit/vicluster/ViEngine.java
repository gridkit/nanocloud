package org.gridkit.vicluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSessionWrapper;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.CloudContext.ServiceKey;
import org.gridkit.vicluster.CloudContext.ServiceProvider;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.zerormi.zlog.LogStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ViEngine {

    public enum Phase {
        PRE_INIT,
        POST_INIT,
        PRE_KILL,
        PRE_SHUTDOWN,
        POST_SHUTDOWN
    }

    public ViSpiConfig getConfig();

    public boolean isStarted();

    public boolean isRunning();

    public boolean isTerminated();

    public void kill();

    public void shutdown();

    public Object getPragma(String key);

    public void setPragmas(Map<String, Object> pragmas);

    public static class Core implements ViEngine {

        private Map<String, Object> coreConfig;
        private ViConf spiConfig;
        private Map<String, PragmaHandler> pragmaHandlers = new HashMap<String, PragmaHandler>();

        private LogStream trace;

        private boolean started;

        private boolean killpending;
        private boolean terminated;

        private FutureBox<Exception> epitaph = new FutureBox<Exception>();

        @Override
        public ViSpiConfig getConfig() {
            return spiConfig;
        }

        @Override
        public boolean isStarted() {
            return started;
        }

        @Override
        public boolean isRunning() {
            return started && !terminated;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }

        @Override
        public Object getPragma(final String key) {
            String pq = ViConf.getPragmaQualifier(key);
            if (pq == null) {
                return invokeRemotely(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return System.getProperty(key);
                    }
                });
            }
            PragmaHandler handler = pragmaHandlers.get(pq);
            if (handler != null) {
                return handler.get(key, this);
            }
            else {
                return coreConfig.get(key);
            }
        }

        @Override
        public void setPragmas(Map<String, Object> pragmas) {
            applyPragmas(pragmas, false);
        }

        public synchronized void ignite(Map<String, Object> config) {
            coreConfig = new LinkedHashMap<String, Object>(config);
            coreConfig.put(ViConf.SPI_KILL_SWITCH, new Runnable() {
                @Override
                public void run() {
                    triggerKillSwitch();
                }
            });
            coreConfig.put(ViConf.SPI_EPITAPH, epitaph);

            if ("true".equalsIgnoreCase(String.valueOf(coreConfig.get(ViConf.NODE_TRACE)))) {
                trace = new InfoStream(LoggerFactory.getLogger(ViEngine.class));
            }

            if (trace != null) {
                trace.log("ViNode initial config");
                for(String key: coreConfig.keySet()) {
                    dumpKeyState(trace, key);
                }
            }

            if (trace != null) {
                trace.log("ViNode phase PRE_INIT");
            }
            processPhase(Phase.PRE_INIT);

            pragmaHandlers.put("pragma-handler", new InitTimePragmaHandler());
            pragmaHandlers.put("type-handler", new InitTimePragmaHandler());

            for(String key: coreConfig.keySet()) {
                if (key.startsWith(ViConf.PRAGMA_HANDLER)) {
                    String pragma = key.substring(ViConf.PRAGMA_HANDLER.length());
                    PragmaHandler handler = (PragmaHandler)coreConfig.get(key);
                    pragmaHandlers.put(pragma, handler);
                }
            }

            executeHooks(coreConfig, false, false);
            if (killpending) {
                started = true;
                kill();
            }
            else {
                try {
                    applyPragmas(new LinkedHashMap<String, Object>(coreConfig), true);

                    if (trace != null) {
                         trace.log("ViNode phase POST_INIT");
                    }

                    processPhase(Phase.POST_INIT);
                    executeHooks(coreConfig, false, false);
                    started = true;
                }
                catch(Exception e) {
                    epitaph.setErrorIfWaiting(e);
                    coreConfig.put(ViConf.ERROR_NODE_BOOTSTRAP, e);
                    started = true;
                    killpending = true;
                }
                if (killpending) {
                    kill();
                }
            }
        }

        private void applyPragmas(Map<String, Object> pragmas, boolean ignoreSharp) {
            while(true) {
                PragmaInvokationContext pctx = new PragmaInvokationContext();
                Map<String, String> props = new HashMap<String, String>();
                for(String p: pragmas.keySet()) {
                    if (ViConf.isVanilaProp(p)) {
                        props.put(p, (String)pragmas.get(p));
                    }
                    else if (p.startsWith("#")) {
                        if (!ignoreSharp) {
                            throw new IllegalArgumentException("Sharp key '" + p + "' cannot be updated");
                        }
                    }
                    else {
                        sendProps(props);
                        String pq = ViConf.getPragmaQualifier(p);
                        PragmaHandler handler = pragmaHandlers.get(pq);
                        if (handler == null) {
                            throw new IllegalArgumentException("No handler for pargma '" + p + "' is found");
                        }
                        else {
                            handler.set(p, pragmas.get(p), this, pctx);
                        }
                    }
                }
                sendProps(props);
                pctx.delta.keySet().removeAll(pragmas.keySet());
                if (pctx.delta.isEmpty()) {
                    break;
                }
                else {
                    pragmas = pctx.delta;
                }
            }
        }

        private void sendProps(Map<String, String> props) {
            if (props.isEmpty()) {
                return;
            }
            final Map<String, String> pp = new HashMap<String, String>(props);
            props.clear();
            invokeRemotely(new Runnable() {
                @Override
                public void run() {
                    for(String p: pp.keySet()) {
                        if (pp.get(p) == null) {
                            System.getProperties().remove(p);
                        }
                        else {
                            System.setProperty(p, pp.get(p));
                        }
                    }
                }
            });
        }

        protected synchronized void triggerKillSwitch() {
            if (!started) {
                killpending = true;
            }
            else {
                kill();
            }
        }

        @Override
		public synchronized void kill() {
            if (!started) {
                throw new IllegalStateException();
            }
            if (!terminated) {
                processPhase(Phase.PRE_KILL);
                executeHooks(coreConfig, true, false);
                // running finalizers
                executeHooks(coreConfig, true, true);
                runCleanupHooks();
            }
        }

        @Override
		public synchronized void shutdown() {
            if (!started) {
                throw new IllegalSelectorException();
            }
            if (!terminated) {
                processPhase(Phase.PRE_SHUTDOWN);
                executeHooks(coreConfig, true, false);
                // running finalizers
                executeHooks(coreConfig, true, true);
                runCleanupHooks();
            }
        }

        private synchronized void runCleanupHooks() {
            if (!terminated) {
                processPhase(Phase.POST_SHUTDOWN);
                executeHooks(coreConfig, true, true);
                try {
                    epitaph.setData(null);
                }
                catch(IllegalStateException e) {
                    // ignore;
                }
                terminated = true;
            }
        }

        private void processPhase(Phase phase) {
            ViEngineGame game = new ViEngineGame(coreConfig);
            game.play(phase, trace);
            coreConfig = game.exportConfig();
            spiConfig = new ViConf(coreConfig);
        }

        private void executeHooks(Map<String, Object> config, boolean reverseOrder, boolean runFinalizers) {

            List<String> keySet = new ArrayList<String>(config.keySet());

            if (reverseOrder) {
                Collections.reverse(keySet);
            }

            for(String key: keySet) {
                if (key.startsWith(ViConf.ACTIVATED_REMOTE_HOOK)) {
                    Object hook = config.get(key);
                    config.remove(key);
                    if (hook != null) {
                        if (hook instanceof Runnable) {
                            invokeRemotely((Runnable)hook);
                        }
                        else {
                            throw new IllegalArgumentException("Hook " + key + " is not a Runnable");
                        }
                    }
                }
                else if (key.startsWith(ViConf.ACTIVATED_HOST_HOOK)) {
                    Object hook = config.get(key);
                    config.remove(key);
                    if (hook != null) {
                        if (hook instanceof Runnable) {
                            ((Runnable)hook).run();
                        }
                        else {
                            throw new IllegalArgumentException("Hook " + key + " is not a Runnable");
                        }
                    }
                }
                else if (runFinalizers && key.startsWith(ViConf.ACTIVATED_FINALIZER_HOOK)) {
                    Object hook = config.get(key);
                    config.remove(key);
                    if (hook != null) {
                        if (hook instanceof Runnable) {
                            ((Runnable)hook).run();
                        }
                        else {
                            throw new IllegalArgumentException("Hook " + key + " is not a Runnable");
                        }
                    }
                }
            }
        }

        private synchronized void invokeRemotely(Runnable hook) {
            if (terminated) {
                return;
            }
            // TODO error handling and logging
            try {
                spiConfig.getManagedProcess().getExecutionService().submit(hook).get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }

        private synchronized <T> T invokeRemotely(Callable<T> task) {
            if (terminated) {
                throw new RejectedExecutionException("Node is terminated");
            }
            // TODO error handling and logging
            try {
                return spiConfig.getManagedProcess().getExecutionService().submit(task).get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public static ViSpiConfig asSpiConfig(final Map<String, Object> config) {
            return new ViConf(config);
        }

        public static void addRule(QuorumGame game, InductiveRule rule) {
            game.rerunOnQuorum(new InductiveRuleHook(new AtomicBoolean(false), rule, false));
        }

        public static <T> Interceptor newSingletonInjector(final String configKey, final ServiceKey<T> key, final ServiceProvider<T> provider) {
            return new Interceptor() {

                @Override
                public void process(String name, Phase phase, QuorumGame game) {
                    CloudContext context = game.getCloudContext();
                    T service = provider == null ? context.lookup(key) : context.lookup(key, provider);
                    if (service != null) {
                        game.setProp(configKey, service);
                    }
                }

                @Override
                public void processAdHoc(String name, ViExecutor node) {
                    throw new IllegalArgumentException("Node is already initialized");
                }
            };
        }

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

        @Deprecated
        public static void processStartupHooks(ViNodeConfig conf, AdvancedExecutor exec) {
            ViEngineGame game = new ViEngineGame(conf.config);
            game.play(Phase.POST_INIT, null);
            Map<String, Object> result = game.exportConfig();
            processHooks(result, exec, false);
        }

        @Deprecated
        public static void processShutdownHooks(ViNodeConfig conf, AdvancedExecutor exec) {
            ViEngineGame game = new ViEngineGame(conf.config);
            game.play(Phase.PRE_SHUTDOWN, null);
            Map<String, Object> result = game.exportConfig();
            processHooks(result, exec, true);
        }

        protected static void processHooks(Map<String, Object> config, AdvancedExecutor target, boolean reverseOrder) {
            List<String> keySet = new ArrayList<String>(config.keySet());

            if (reverseOrder) {
                Collections.reverse(keySet);
            }

            for(String key: keySet) {
                if (key.startsWith(ViConf.ACTIVATED_REMOTE_HOOK)) {
                    Object hook = config.get(key);
                    config.remove(key);
                    if (hook != null) {
                        if (hook instanceof Runnable) {
                            MassExec.exec(target, (Runnable)hook);
                        }
                        else {
                            throw new IllegalArgumentException("Hook " + key + " is not a Runnable");
                        }
                    }
                }
                else if (key.startsWith(ViConf.ACTIVATED_HOST_HOOK)) {
                    Object hook = config.get(key);
                    config.remove(key);
                    if (hook != null) {
                        if (hook instanceof Runnable) {
                            ((Runnable)hook).run();
                        }
                        else {
                            throw new IllegalArgumentException("Hook " + key + " is not a Runnable");
                        }
                    }
                }
            }
        }

        public void dumpCore(LogStream logger) {
            logger.log("ViNode state dump");
            for(String key: coreConfig.keySet()) {
                dumpKeyState(logger, key);
            }
        }

        protected void dumpKeyState(LogStream logger, String key) {
            Object v = coreConfig.get(key);
            if (v instanceof String) {
                logger.log("  " + key + ": " + v);
            }
            else if (v instanceof Future<?>) {
                Future<?> f = (Future<?>) v;
                if (f.isDone()) {
                    try {
                        logger.log("  " + key + " -> " + f.get());
                    }
                    catch(ExecutionException e) {
                        logger.log("  " + key + " [!] " + e.getCause());
                    }
                    catch(Exception e) {
                        logger.log("  " + key + " [!] " + e);
                    }
                }
                else {
                    logger.log("  " + key + " - (unset future)");
                }
            }
            else if (v == null) {
                logger.log("  " + key + " - null");
            }
            else {
                logger.log("  " + key + ": " + v);
            }
        }

        private class PragmaInvokationContext implements WritableSpiConfig {

            Map<String, Object> delta = new LinkedHashMap<String, Object>();

            @Override
            public void unsetProp(String propName) {
                setProp(propName, null);
            }

            @Override
            public void addUniqueProp(String propName, Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setProp(String propName, Object value) {
                if (propName.startsWith("#")) {
                    coreConfig.put(propName, value);
                }
                else {
                    coreConfig.put(propName, value);
                    delta.put(propName, value);
                }
            }
        }
    }

    public static abstract class SpiPropsWrapper implements ViSpiConfig {

        protected abstract ViSpiConfig getConfig();

        @Override
        public Map<String, Object> getConfigMap() {
            return getConfig().getConfigMap();
        }

        @Override
        public <T> T get(String key) {
            return getConfig().get(key);
        }

        @Override
        public String getNodeName() {
            return getConfig().getNodeName();
        }

        @Override
        public String getNodeType() {
            return getConfig().getNodeType();
        }

        @Override
        public boolean isConfigTraceEnbaled() {
            return getConfig().isConfigTraceEnbaled();
        }

        @Override
        public boolean shouldDumpConfigOnFailure() {
            return getConfig().shouldDumpConfigOnFailure();
        }

        @Override
        public CloudContext getCloudContext() {
            return getConfig().getCloudContext();
        }

        @Override
        public HostControlConsole getControlConsole() {
            return getConfig().getControlConsole();
        }

        @Override
        public ProcessLauncher getProcessLauncher() {
            return getConfig().getProcessLauncher();
        }

        @Override
        public RemoteExecutionSession getRemotingSession() {
            return getConfig().getRemotingSession();
        }

        @Override
        public RemoteExecutionSessionWrapper getInstrumentationWrapper() {
            return getConfig().getInstrumentationWrapper();
        }

        @Override
        public boolean isInstrumentationWrapperApplied() {
            return getConfig().isInstrumentationWrapperApplied();
        }

        @Override
        public String getJvmExecCmd() {
            return getConfig().getJvmExecCmd();
        }

        @Override
        public List<String> getSlaveShallowClasspath() {
            return getConfig().getSlaveShallowClasspath();
        }

        @Override
        public List<ClasspathEntry> getSlaveClasspath() {
            return getConfig().getSlaveClasspath();
        }

        @Override
        public List<AgentEntry> getSlaveAgents() {
            return getConfig().getSlaveAgents();
        }

        @Override
        public List<String> getSlaveArgs() {
            return getConfig().getSlaveArgs();
        }

        @Override
        public Map<String, String> getSlaveEnv() {
            return getConfig().getSlaveEnv();
        }

        @Override
        public String getSlaveWorkDir() {
            return getConfig().getSlaveWorkDir();
        }

        @Override
        public ManagedProcess getManagedProcess() {
            return getConfig().getManagedProcess();
        }

        @Override
        public NodeFactory getNodeFactory() {
            return getConfig().getNodeFactory();
        }

        @Override
        public ViNode getNodeInstance() {
            return getConfig().getNodeInstance();
        }

        @Override
        public StreamCopyService getStreamCopyService() {
            return getConfig().getStreamCopyService();
        }
    }

    public interface QuorumGame extends ViSpiConfig, WritableSpiConfig {

        public String getStringProp(String propName);

        void setPropIfAbsent(String propName, Object value);

        Map<String, Object> getAllConfigProps();

        public Object getProp(String propName);

        public Map<String, Object> getConfigProps(String matcher);

        @Override
		public void unsetProp(String propName);

        @Override
		public void addUniqueProp(String propName, Object value);

        @Override
		public void setProp(String propName, Object value);

        /**
         * Will update value without reordering of keys.
         */
        public void replaceProp(String propName, Object value);

        /**
         * Will remove oldName placing newName in its place in key order.
         */
        public void replaceProp(String oldName, String newName, Object value);

        public void rerunOnUpdate(Rerun rerun);

        public void rerunOnQuorum(Rerun rerun);

    }

    public interface Rerun {

        public void rerun(QuorumGame game, Map<String, Object> changes);

    }

    public interface Interceptor {

        public void process(String name, Phase phase, QuorumGame game);

        public void processAdHoc(String name, ViExecutor node);

    }

    public interface WritableSpiConfig {

        public void unsetProp(String propName);

        public void addUniqueProp(String propName, Object value);

        public void setProp(String propName, Object value);

    }

    public interface PragmaHandler {

        public Object get(String key, ViEngine engine);

        public void set(String key, Object value, ViEngine engine, WritableSpiConfig writableConfig);

    }

    public interface BulkPargmaSetter extends PragmaHandler {

        public void setAll(Map<String, Object> settings, ViEngine engine, WritableSpiConfig writableConfig);

    }

    public class InitTimePragmaHandler implements PragmaHandler {

        @Override
		public Object get(String key, ViEngine engine) {
            return engine.getConfig().get(key);
        }

        @Override
		public void set(String key, Object value, ViEngine engine, WritableSpiConfig wc) {
            if (engine.isStarted()) {
                throw new IllegalStateException("Pragma '" + key + "' cannot be set on started node");
            }
        }
    }

    public class ReadOnlyPragmaHandler implements PragmaHandler {

        @Override
		public Object get(String key, ViEngine engine) {
            return engine.getConfig().get(key);
        }

        @Override
		public void set(String key, Object value, ViEngine engine, WritableSpiConfig wc) {
            throw new IllegalStateException("Pragma '" + key + "' is read only");
        }
    }

    public class HookPragmaHandler implements PragmaHandler {

        @Override
		public Object get(String key, ViEngine engine) {
            return engine.getConfig().get(key);
        }

        @Override
		public void set(String key, Object value, ViEngine engine, WritableSpiConfig wc) {
            Interceptor hook = (Interceptor) value;
            if (engine.isStarted()) {
                ViExecutor exec = null;
                ManagedProcess mp = engine.getConfig().getManagedProcess();
                if (mp != null) {
                    exec = new AdvExecutor2ViExecutor(mp.getExecutionService());
                }
                hook.processAdHoc(key, exec);
            }
            wc.setProp(key, value);
        }
    }

    public static class InductiveRuleHook implements Rerun {

        private AtomicBoolean done;
        private InductiveRule rule;
        private boolean lastChance;

        public InductiveRuleHook(AtomicBoolean done, InductiveRule rule, boolean lastChance) {
            this.done = done;
            this.rule = rule;
            this.lastChance = lastChance;
        }

        @Override
        public void rerun(QuorumGame game, Map<String, Object> changes) {
            if (changes != null) {
                lastChance = false;
            }
            if (done.get()) {
                return;
            }
            if (rule.apply(game)) {
                done.set(true);
            }
            else {
                game.rerunOnUpdate(this);
                if (!lastChance) {
                    lastChance = true;
                }
                else {
                    game.rerunOnQuorum(this);
                }
            }
        }
    }

    public static interface InductiveRule {

        public boolean apply(QuorumGame game);

    }

    public static class DefaultInitRuleSet implements Interceptor {

        @Override
        public void process(String name, Phase phase, QuorumGame game) {
            if (phase == Phase.PRE_INIT) {

                String type = game.getStringProp(ViConf.NODE_TYPE);
                if (type == null) {
                    throw new IllegalArgumentException("Node type is not defined");
                }
                InductiveRule rule = (InductiveRule) game.getProp(ViConf.TYPE_HANDLER + type);
                if (rule == null) {
                    throw new IllegalArgumentException("Handler for type '" + type + "' is not found");
                }
                if (!rule.apply(game)) {
                    throw new IllegalArgumentException("Type initilizer " + rule + " has failed");
                }
            }
        }

        @Override
        public void processAdHoc(String name, ViExecutor node) {
            throw new IllegalStateException("Node '" + node + "' is already initialized");
        }
    }

    public static class TypeInitializerRule implements InductiveRule {

        @Override
        public boolean apply(QuorumGame game) {
            String type = game.getStringProp(ViConf.NODE_TYPE);
            if (type == null) {
                return false;
            }
            InductiveRule rule = (InductiveRule) game.getProp(ViConf.TYPE_HANDLER + type);
            if (rule == null) {
                return false;
            }
            Core.addRule(game, rule);
            return true;
        }
    }

    public static class RuleSet implements Interceptor, Rerun {

        private final List<Interceptor> interceptors = new ArrayList<ViEngine.Interceptor>();

        public RuleSet(Interceptor... interceptors) {
            for(Interceptor i : interceptors) {
                this.interceptors.add(i);
            }
        }

        @Override
        public void rerun(QuorumGame game, Map<String, Object> changes) {
            for(Interceptor i: interceptors) {
                if (i instanceof Rerun) {
                    ((Rerun) i).rerun(game, changes);
                }
            }
        }

        @Override
        public void process(String name, Phase phase, QuorumGame game) {
            for(Interceptor i: interceptors) {
                i.process(name, phase, game);
            }
        }

        @Override
        public void processAdHoc(String name, ViExecutor node) {
            for(Interceptor i: interceptors) {
                i.processAdHoc(name, node);
            }
        }
    }

    public static abstract class IdempotentConfigBuilder<T> implements Interceptor, Rerun {

        protected String configKey;

        protected abstract T buildState(QuorumGame game);

        protected boolean sameState(T oldState, T newState) {
            return oldState == null ? newState == null : oldState.equals(newState);
        }

        public IdempotentConfigBuilder(String configKey) {
            this.configKey = configKey;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void rerun(QuorumGame game, Map<String, Object> changes) {
            game.rerunOnUpdate(this);
            if (!changes.isEmpty()) {
                T oldV = (T)game.getProp(configKey);
                T newV = buildState(game);
                if (!sameState(oldV, newV)) {
                    game.setProp(configKey, buildState(game));
                }
            }
        }

        @Override
        public void process(String name, Phase phase, QuorumGame game) {
            if (phase == Phase.PRE_INIT) {
                game.setProp(configKey, buildState(game));
                game.rerunOnUpdate(this);
            }
        }

        @Override
        public void processAdHoc(String name, ViExecutor node) {
            throw new IllegalArgumentException("Node is already initialized");
        }
    }

    public static abstract class ControlConsoleInitialzer implements InductiveRule {

        @Override
        public boolean apply(QuorumGame game) {
            game.setPropIfAbsent(ViConf.SPI_CONTROL_CONSOLE, getControlConsole());
            return true;
        }

        protected abstract HostControlConsole getControlConsole();

    }

    static class InfoStream implements LogStream {

        private Logger logger;

        public InfoStream(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public void log(String message) {
            logger.info(message);
        }

        @Override
        public void log(Throwable e) {
            logger.info(e.toString(), e);
        }

        @Override
        public void log(String message, Throwable e) {
            logger.info(message, e);
        }

        @Override
        public void log(String format, Object... argument) {
            logger.info(format, argument);
        }
    }
}
