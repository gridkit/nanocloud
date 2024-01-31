package org.gridkit.vicluster.telecontrol;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher.LaunchConfig;
import org.gridkit.nanocloud.telecontrol.ProcessSporeLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSessionWrapper;
import org.gridkit.nanocloud.telecontrol.ZeroRmiRemoteSession;
import org.gridkit.nanocloud.viengine.ProcessLifecycleListener;
import org.gridkit.vicluster.CloudContext;
import org.gridkit.vicluster.CloudContext.Helper;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.IdempotentConfigBuilder;
import org.gridkit.vicluster.ViEngine.InductiveRule;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;

public abstract class GenericNodeTypeHandler implements ViEngine.InductiveRule {

    @Override
    public boolean apply(QuorumGame game) {
        initNodeCommon(game);
        initJvmConfigurationRules(game);
        initExtraConfigurationRules(game);
        initControlConsole(game);
        initProcessLauncher(game);

        initRemoting(game);
        initInstrumentationWrapper(game);
        initProcessBootstrapper(game);

        return true;
    }

    private void initNodeCommon(QuorumGame game) {
        game.setPropIfAbsent(ViConf.SPI_STREAM_COPY_SERVICE, getCloudSingleton(game, StreamCopyThread.class, "shutdown"));
    }

    protected void initExtraConfigurationRules(QuorumGame game) {
    }

    protected void initProcessBootstrapper(QuorumGame game) {
        ViEngine.Core.addRule(game, createProcessBootstrapperRule());
    }

    protected void initRemoting(QuorumGame game) {
        ViEngine.Core.addRule(game, createRemotingConfigurationRule());
    }

    protected void initInstrumentationWrapper(QuorumGame game) {
        ViEngine.Core.addRule(game, createInstrumentationWrapperRule());
    }

    protected void initProcessLauncher(QuorumGame game) {
        game.setPropIfAbsent(ViConf.SPI_PROCESS_LAUNCHER, createProcessLauncher(game));
    }

    protected void initControlConsole(QuorumGame game) {
        game.setPropIfAbsent(ViConf.SPI_CONTROL_CONSOLE, createControlConsole(game));
    }

    protected void initJvmConfigurationRules(QuorumGame game) {
        game.setPropIfAbsent(ViConf.HOOK_CLASSPATH_BUILDER, createClasspathBuilder(game));
        game.setPropIfAbsent(ViConf.HOOK_JVM_ARGUMENTS_BUIDLER, createJvmArgumentsBuilder(game));
        game.setPropIfAbsent(ViConf.HOOK_JVM_ENV_VARS_BUIDLER, createJvmEnvironmentBuilder(game));
        game.setPropIfAbsent(ViConf.HOOK_AGENT_BUILDER, createAgentBuilder(game));
        game.setPropIfAbsent(ViConf.JVM_EXEC_CMD, defaultJavaExecCmd(game));
    }

    protected abstract String defaultJavaExecCmd(QuorumGame game);

    protected Interceptor createClasspathBuilder(QuorumGame game) {
        return new ClasspathReplicaBuilder();
    }

    protected Interceptor createAgentBuilder(QuorumGame game) {
        return new AgentBuilder();
    }

    protected Interceptor createJvmArgumentsBuilder(QuorumGame game) {
        return new JvmArgumentBuilder();
    }

    protected Interceptor createJvmEnvironmentBuilder(QuorumGame game) {
        return new EnvironmentBuilder();
    }

    protected <T> T getCloudSingleton(QuorumGame game, Class<T> type, String shutdownMethod) {
        T instance = game.getCloudContext().lookup(Helper.key(type), CloudContext.Helper.reflectionProvider(type, shutdownMethod));
        return instance;
    }

    protected abstract HostControlConsole createControlConsole(QuorumGame game);

    protected ProcessLauncher createProcessLauncher(QuorumGame game) {
        return getCloudSingleton(game, ProcessSporeLauncher.class, null);
    }

    protected InductiveRule createRemotingConfigurationRule() {
        return new ZeroRmiConfigurationRule();
    }

    protected InductiveRule createInstrumentationWrapperRule() {
        return new InstrumentationWrapperRule();
    }

    protected InductiveRule createProcessBootstrapperRule() {
        return new ProcessLauncherRule();
    }

    protected InductiveRule createNodeProducerRule() {
        return new NodeProductionRule();
    }

    public static class ZeroRmiConfigurationRule implements InductiveRule {

        @Override
        public boolean apply(QuorumGame game) {
            if (	game.getRemotingSession() == null
                &&	game.getProcessLauncher() != null
                &&  game.getControlConsole() != null) {

                String nodeName = game.getStringProp(ViConf.NODE_NAME);
                ZeroRmiRemoteSession session = new ZeroRmiRemoteSession(nodeName);
                game.setProp(ViConf.SPI_REMOTING_SESSION, session);
                game.unsetProp(ViConf.SPI_INSTRUMENTATION_WRAPPER_APPLIED);

                return true;
            }
            else {
                return false;
            }
        }
    }

    public static class InstrumentationWrapperRule implements InductiveRule {

        @Override
        public boolean apply(QuorumGame game) {
            if (	game.getRemotingSession() != null
                &&	game.getInstrumentationWrapper() != null
                &&  !game.isInstrumentationWrapperApplied()) {

                RemoteExecutionSession session = game.getRemotingSession();
                RemoteExecutionSessionWrapper wrapper = game.getInstrumentationWrapper();
                RemoteExecutionSession ws = wrapper.wrap(session);

                game.setProp(ViConf.SPI_REMOTING_SESSION, ws);
                game.unsetProp(ViConf.SPI_INSTRUMENTATION_WRAPPER_APPLIED);

                return true;
            }
            else {
                return false;
            }
        }
    }

    public static class ClasspathReplicaBuilder extends IdempotentConfigBuilder<List<ClasspathEntry>> {

        private static final String IS_GRIDKIT_JAR = "isGridkitJar";

        public ClasspathReplicaBuilder() {
            super(ViConf.SPI_SLAVE_CLASSPATH);
        }

        @Override
        protected List<ClasspathEntry> buildState(QuorumGame game) {
            try {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Map<String, String> tweaks = (Map)game.getConfigProps(ViConf.CLASSPATH_TWEAK);
                final boolean inheritClassPath = !Boolean.FALSE.toString().equals(game.getAllConfigProps().get(ViConf.CLASSPATH_INHERIT));
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
                    List<ClassPathTweak> classPathTweaks = new ArrayList<ClassPathTweak>(tweaks.size());
                    for (String tweak : tweaks.values()) {
                        classPathTweaks.add(new ClassPathTweak(tweak));
                    }
                    Collections.sort(classPathTweaks);

                    List<ClasspathEntry> inheritedEntries = new ArrayList<Classpath.ClasspathEntry>(cp);
                    List<ClasspathEntry> tweaksEntries = new ArrayList<Classpath.ClasspathEntry>();

                    for(ClassPathTweak k: classPathTweaks) {
                        if (k.isAddition) {
                            addEntry(tweaksEntries, k.classPathEntry);
                        }
                        else {
                            removeEntry(inheritedEntries, k.classPathEntry);
                            removeEntry(tweaksEntries, k.classPathEntry);
                        }
                    }

                    tweaksEntries.addAll(inheritedEntries); // add filtered inherited entries to the end of class-path

                    return tweaksEntries;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean isGridKitClasses(ClasspathEntry classpathEntry){
            Boolean cachedResult = classpathEntry.getMark(IS_GRIDKIT_JAR);
            if (cachedResult != null) {
                return cachedResult;
            }
            File file = classpathEntry.getLocalFile();
            try {
                if (file == null) {
                    boolean result = false;
                    ZipInputStream zipInputStream = new ZipInputStream(classpathEntry.getContent());
                    ZipEntry entry;
                    while ((entry = zipInputStream.getNextEntry()) != null) {
                        if (entry.getName().startsWith("org/gridkit/")) {
                            result = true;
                            break;
                        }
                    }
                    try {
                        zipInputStream.close();
                    }
                    catch(IOException e) {
                        // ignore;
                    }
                    classpathEntry.setMark(IS_GRIDKIT_JAR, result);
                    return result;
                }
                else {
                    boolean result = false;
                    if (file.isFile()) {
                        try (ZipFile zipFile = new ZipFile(file)) {
                            ZipEntry gridKitEntry = zipFile.getEntry("org/gridkit");
                            result =  gridKitEntry != null;
                        }
                    } else if (file.isDirectory()) {
                        File gridKitPackage = new File(new File(file, "org"), "gridkit");
                        result = gridKitPackage.isDirectory();
                    }
                    classpathEntry.setMark(IS_GRIDKIT_JAR, result);
                    return result;
                }
            } catch (IOException e) {
                // ignore
            }
            return false;
        }

        private boolean isTestClasses(ClasspathEntry classpathEntry){
            return classpathEntry.getFileName().contains("test-classes");
        }

        private void addEntry(List<ClasspathEntry> entries, String path) throws IOException {
            ClasspathEntry entry = Classpath.getLocalEntry(path);
            if (entry != null) {
                entries.add(entry);
            }
        }

        private void removeEntry(List<ClasspathEntry> entries, String path) {
            Iterator<ClasspathEntry> it = entries.iterator();
            while(it.hasNext()) {
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

        @Override
        protected boolean sameState(List<ClasspathEntry> existing, List<ClasspathEntry> rebuilt) {
            if (existing.size() != rebuilt.size()) {
                return false;
            }
            else {
                for(int i = 0; i != existing.size(); ++i) {
                    ClasspathEntry e1 = existing.get(0);
                    ClasspathEntry e2 = rebuilt.get(0);
                    if (e1.getLocalFile() == null && e2.getLocalFile() == null) {
                        if (!compareContent(e1, e2)) {
                            return false;
                        }
                    }
                    else if (e1.getLocalFile() == null || e2.getLocalFile() == null) {
                        return false;
                    }
                    else {
                        File p1 = e1.getLocalFile();
                        File p2 = e2.getLocalFile();
                        if (!p1.getPath().equals(p2.getPath())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private boolean compareContent(ClasspathEntry e1, ClasspathEntry e2) {
            return e1.getContentHash().equals(e2.getContentHash());
        }

        private class ClassPathTweak implements Comparable<ClassPathTweak>{

            private final int priority;
            private final boolean isAddition;
            private final String classPathEntry;

            public ClassPathTweak(String tweak) {
                final int endOfPriorityPart = tweak.indexOf("!");
                final char action = tweak.charAt(endOfPriorityPart + 1);
                priority = Integer.parseInt(tweak.substring(0, endOfPriorityPart));
                if (action == '+') {
                    isAddition = true;
                } else if (action == '-') {
                    isAddition = false;
                } else {
                    throw new AssertionError("Invalid action in tweak: " + tweak);
                }
                classPathEntry = normalize(toURL(tweak.substring(endOfPriorityPart + 2)));
            }

            @Override
            public int compareTo(ClassPathTweak o) {
                return Integer.valueOf(this.priority).compareTo(o.priority);
            }
        }
    }

    public static class ShallowClasspathBuilder extends IdempotentConfigBuilder<List<String>> {

        public ShallowClasspathBuilder() {
            super(ViConf.SPI_SLAVE_SHALLOW_CLASSPATH);
        }

        @Override
        protected List<String> buildState(QuorumGame game) {
            boolean useShallow = !Boolean.FALSE.toString().equalsIgnoreCase(game.<String>get(ViConf.CLASSPATH_USE_SHALLOW));
            boolean inheright = !Boolean.FALSE.toString().equalsIgnoreCase(game.<String>get(ViConf.CLASSPATH_INHERIT));
            boolean hasTweak = !game.getConfigProps(ViConf.CLASSPATH_TWEAK).isEmpty();

            if (inheright && !hasTweak && useShallow) {
                return ClasspathUtils.getStartupClasspath();
            }
            else {
                return null;
            }
        }
    }

    public static class ClasspathReplicaBuilderLocal extends ClasspathReplicaBuilder {

        @Override
        protected List<ClasspathEntry> buildState(QuorumGame game) {
            boolean useShallow = !Boolean.FALSE.toString().equalsIgnoreCase(game.<String>get(ViConf.CLASSPATH_USE_SHALLOW));
            boolean inheright = !Boolean.FALSE.toString().equalsIgnoreCase(game.<String>get(ViConf.CLASSPATH_INHERIT));
            boolean hasTweak = !game.getConfigProps(ViConf.CLASSPATH_TWEAK).isEmpty();

            if (inheright && !hasTweak && useShallow) {
                return Collections.emptyList();
            }
            else {
                return super.buildState(game);
            }
        }
    }

    public static class AgentBuilder extends IdempotentConfigBuilder<List<AgentEntry>> {

        public AgentBuilder() {
            super(ViConf.SPI_SLAVE_AGENT);
        }

        @Override
        protected List<AgentEntry> buildState(QuorumGame game) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Map<String, String> agents = (Map<String, String>) (Map) game.getConfigProps(ViConf.JVM_AGENT);
            List<AgentEntry> agentEntries = new ArrayList<AgentEntry>();
            for (String agentAndOptions : agents.values()) {
                final int delimiterIndex = agentAndOptions.indexOf("=");
                final File file = new File(agentAndOptions.substring(0, delimiterIndex));
                if (!file.exists()) {
                    throw new IllegalArgumentException("Can not find agent file " + file);
                }
                final String options = agentAndOptions.substring(delimiterIndex + 1);
                final AgentEntry agentEntry = new AgentEntry(file, options.isEmpty() ? null : options);
                agentEntries.add(agentEntry);
            }
            return agentEntries;
        }
    }

    public static class JvmArgumentBuilder extends IdempotentConfigBuilder<List<String>> {

        public JvmArgumentBuilder() {
            super(ViConf.SPI_SLAVE_ARGS);
        }

        @Override
        protected List<String> buildState(QuorumGame game) {
            Map<String, Object> cmd = game.getConfigProps(ViConf.JVM_ARGUMENT);
            List<String> options = new ArrayList<String>();
            for(Object v: cmd.values()) {
                String o = (String) v;
                if (o.startsWith("|")) {
                    String[] opts = o.split("\\|");
                    for(String oo: opts) {
                        addOption(options, oo);
                    }
                }
                else {
                    addOption(options, o);
                }
            }
            return options;
        }

        private void addOption(List<String> options, String o) {
            o = o.trim();
            if (o.length() > 0) {
                options.add(o);
            }
        }
    }

    public static class EnvironmentBuilder extends IdempotentConfigBuilder<Map<String, String>> {

        public EnvironmentBuilder() {
            super(ViConf.SPI_SLAVE_ENV);
        }

        @Override
        protected Map<String, String> buildState(QuorumGame game) {
            Map<String, Object> props = game.getConfigProps(ViConf.JVM_ENV_VAR);
            Map<String, String> env = new LinkedHashMap<String, String>();
            for(String k: props.keySet()) {
                String vn = k.substring(ViConf.JVM_ENV_VAR.length());
                String vv = (String) props.get(k);
                if (vv.length() == 1 && vv.charAt(0) == '\00') {
                    vv = null;
                }
                env.put(vn, vv);
            }
            return env.isEmpty() ? null : env;
        }
    }

    public static class ProcessLauncherRule implements InductiveRule {

        @Override
        public boolean apply(QuorumGame game) {
            if (
                    game.getManagedProcess() == null
                &&  game.getControlConsole() != null
                &&  game.getRemotingSession() != null
                &&  game.getProcessLauncher() != null
                &&  game.getJvmExecCmd() != null
                &&  game.getSlaveClasspath() != null
                &&  game.getSlaveArgs() != null)
            {
                LaunchConfig config = new SimpleProcessLauncherConfig(game);
                ProcessLauncher launcher = (ProcessLauncher) game.getProp(ViConf.SPI_PROCESS_LAUNCHER);
                final ManagedProcess mp = launcher.launchProcess(config);
                game.setProp(ViConf.SPI_MANAGED_PROCESS, mp);
                game.addUniqueProp(ViConf.ACTIVATED_FINALIZER_HOOK + "destroy-process", new Runnable() {
                    @Override
                    public void run() {
                        mp.destroy();
                    }
                });
                return true;
            }
            else {
                return false;
            }
        }
    }

    private static class SimpleProcessLauncherConfig implements LaunchConfig {

        private final QuorumGame game;

        public SimpleProcessLauncherConfig(QuorumGame game) {
            this.game = game;
        }

        @Override
        public String getNodeName() {
            return game.getNodeName();
        }

        @Override
        public HostControlConsole getControlConsole() {
            return game.getControlConsole();
        }

        @Override
        public RemoteExecutionSession getRemotingSession() {
            return game.getRemotingSession();
        }

        @Override
        public ProcessLifecycleListener getLifecycleListener() {
            final List<ProcessLifecycleListener> pll = new ArrayList<ProcessLifecycleListener>();
            for (Object pl: game.getConfigProps(ViConf.JVM_PROCESS_LIFECYCLE_LISTENER).values()) {
                if (pl instanceof ProcessLifecycleListener) {
                    pll.add((ProcessLifecycleListener) pl);
                }
            }
            if (pll.isEmpty()) {
                return null;
            } else {
                return new ProcessLifecycleListener() {

                    @Override
                    public void processStarted(String nodeName, ExecInfo execInfo) {
                        for (ProcessLifecycleListener pl: pll) {
                            try {
                                pl.processStarted(nodeName, execInfo);
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }

                    @Override
                    public void processExecFailed(String nodeName, ExecFailedInfo execFailedInfo) {
                        for (ProcessLifecycleListener pl: pll) {
                            try {
                                pl.processExecFailed(nodeName, execFailedInfo);
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }

                    @Override
                    public void processTerminated(String nodeName, TerminationInfo termInfo) {
                        for (ProcessLifecycleListener pl: pll) {
                            try {
                                pl.processTerminated(nodeName, termInfo);
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                };
            }
        }

        @Override
        public String getSlaveJvmExecCmd() {
            return game.getJvmExecCmd();
        }

        @Override
        public String getSlaveWorkDir() {
            return game.getSlaveWorkDir();
        }

        @Override
        public Map<String, String> getSlaveEnv() {
            return game.getSlaveEnv();
        }

        @Override
        public List<String> getSlaveArgs() {
            return game.getSlaveArgs();
        }

        @Override
        public List<String> getSlaveShallowClasspath() {
            return game.getSlaveShallowClasspath();
        }

        @Override
        public List<ClasspathEntry> getSlaveClasspath() {
            return game.getSlaveClasspath();
        }

        @Override
        public List<AgentEntry> getAgentEntries() {
            return game.getSlaveAgents();
        }

        @Override
        public StreamCopyService getStreamCopyService() {
            return game.getStreamCopyService();
        }
    }

    public static class NodeProductionRule implements InductiveRule {

        @Override
        public boolean apply(QuorumGame game) {
            if (
                    game.getNodeInstance() == null
                &&  game.getNodeFactory() != null
                &&  game.getManagedProcess() != null
                )
            {
                NodeFactory factory = game.getNodeFactory();
                ViNode mp = factory.createViNode(game.getAllConfigProps());
                game.setProp(ViConf.SPI_NODE_INSTANCE, mp);
                return true;
            }
            else {
                return false;
            }
        }
    }
}
