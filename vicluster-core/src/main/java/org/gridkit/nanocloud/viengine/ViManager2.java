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
package org.gridkit.nanocloud.viengine;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.NodeExecutionException;
import org.gridkit.nanocloud.VX;
import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViExecutor.CloudRunnable;
import org.gridkit.nanocloud.ViNode;
import org.gridkit.nanocloud.ViNodeExtender;
import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeCore;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.ViNodeSet;
import org.gridkit.vicluster.ViProps;
import org.gridkit.zerormi.DirectRemoteExecutor;
import org.gridkit.zerormi.RemoteExecutor;
import org.gridkit.zerormi.RemoteExecutorAsynAdapter;
import org.gridkit.zerormi.RemoteStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ViManager2 implements Cloud {

    private final static Logger LOGGER = LoggerFactory.getLogger(ViNodeSet.class);

    public static ViNode multiNode(Collection<ViNode> nodes) {
        return MultiNode.group(nodes);
    }

    private Map<String, ManagedNode> liveNodes = new TreeMap<String, ManagedNode>();
    private Map<String, ManagedNode> deadNodes = new TreeMap<String, ManagedNode>();
    private Map<String, NodeSelector> dynamicSelectors = new LinkedHashMap<String, NodeSelector>();

    private ViNodeProvider provider;
    private ExecutorService asyncInitThreads;
    private boolean terminated = false;

    private long ruleCounter = 0;

    public ViManager2(ViNodeProvider provider) {
        this(provider, 32);
    }

    public ViManager2(ViNodeProvider provider, int deferedTaskLimit) {
        this.provider = provider;
        if (deferedTaskLimit == 0) {
            asyncInitThreads = Executors.newSingleThreadExecutor();
        }
        else {
            asyncInitThreads = new ThreadPoolExecutor(deferedTaskLimit, deferedTaskLimit, 100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1024), new ThreadFactory() {

                int counter = 1;

                @Override
                public synchronized Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("ViManager-worker-" + (counter++));
                    t.setDaemon(true);
                    return t;
                }
            });
        }
    }

    public ViNodeProvider getProvider() {
        return provider;
    }

    private void ensureAlive() {
        if (terminated) {
            throw new IllegalStateException("Cloud has been terminated");
        }
    }

    @Override
    public <X> X x(ViConfExtender<X> extender) {
        return nodes("**").x(extender);
    }

    @Override
    public synchronized ViNode node(String namePattern) {
        ensureAlive();
        if (liveNodes.containsKey(namePattern)) {
            return liveNodes.get(namePattern);
        }
        else if (deadNodes.containsKey(namePattern)) {
            return deadNodes.get(namePattern);
        }
        else if (dynamicSelectors.containsKey(namePattern)){
            return dynamicSelectors.get(namePattern);
        }
        else if (isPattern(namePattern)) {
            NodeSelector selector = new NodeSelector(namePattern);
            dynamicSelectors.put(namePattern, selector);
            return selector;
        }
        else {
            String name = namePattern;
            ManagedNode node = new ManagedNode(name);
            inferConfiguration(node);
            liveNodes.put(name, node);
            return node;
        }
    }

    @Override
    public ViNode nodes(String... patterns) {
        ensureAlive();
        Set<ViNode> nodes = new LinkedHashSet<ViNode>();
        for(String pattern: patterns) {
            nodes.add(node(pattern));
        }
        return MultiNode.group(nodes);
    }

    private synchronized void inferConfiguration(ManagedNode node) {
        List<Rule> rules = new ArrayList<ViManager2.Rule>();
        for(NodeSelector selector: dynamicSelectors.values()) {
            if (selector.match(node)) {
                rules.addAll(selector.rules);
            }
        }
        Collections.sort(rules);
        for(Rule rule: rules) {
            rule.getConfig().apply(node);
        }
    }

    private boolean isPattern(String namePattern) {
        return namePattern.indexOf('*') >= 0 || namePattern.indexOf('?') >= 0;
    }

    @Override
    public synchronized Collection<ViNode> listNodes(String namePattern) {
        ensureAlive();
        Set<ViNode> result = new LinkedHashSet<ViNode>();
        Pattern regEx = GlobHelper.translate(namePattern, ".");
        for(ManagedNode vinode: liveNodes.values()) {
            if (match(regEx, vinode)) {
                result.add(vinode);
            }
        }
        return result;
    }

    protected Collection<ViNode> listNodes(Pattern regEx) {
        if (liveNodes.isEmpty()) {
            return Collections.emptyList();
        }
        else {
            List<ViNode> result = new ArrayList<ViNode>();
            for(ManagedNode vinode: liveNodes.values()) {
                if (match(regEx, vinode) ) {
                    result.add(vinode);
                }
            }
            return result;
        }
    }

    private static boolean match(Pattern regEx, ManagedNode vinode) {
        return regEx.matcher(vinode.name).matches()
                || regEx.matcher("." + vinode.name).matches()
                || regEx.matcher(vinode.name + ".").matches()
                || regEx.matcher("." + vinode.name + ".").matches();
    }

    @Override
    public void shutdown() {
        // TODO concurrency design is rather poor for this class
        List<Future<?>> epitaphs;
        synchronized(this) {
            if (terminated == true) {
                return;
            }
            terminated = true;
            epitaphs = new ArrayList<Future<?>>();
            for(final ManagedNode node: liveNodes.values()) {
                epitaphs.add(asyncInitThreads.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            node.shutdown();
                        }
                        catch(Exception e) {
                            LOGGER.warn("Exception on shutdown for '" + node.name + "'", e);
                        }
                    }
                }));
            }
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        for(Future<?> e: epitaphs) {
            try {
                long to = deadline - System.nanoTime();
                if (to <= 0) {
                    to = 1;
                }
                // safety time out, normally node should shutdown promptly
                e.get(to, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ee) {
                break;
            } catch (ExecutionException ee) {
                LOGGER.warn("Exception on shutdown", ee.getCause());
            } catch (TimeoutException e1) {
                LOGGER.warn("Timeout on shutdown");
            }
        }
        // there could be a race between initialization and shutdown here
        asyncInitThreads.shutdown();
        try {
            asyncInitThreads.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("ViManager shutdown: Defered task threads are still active");
        }
        provider.shutdown();
    }

    public synchronized void resetDeadNode() {
        ensureAlive();
        deadNodes.clear();
    }

    protected synchronized void markAsDead(ManagedNode node) {
        liveNodes.remove(node.name);
        deadNodes.put(node.name, node);
    }

    protected synchronized Rule newRule(NodeSelector selector) {
        Rule rule = new Rule(ruleCounter++, selector);
        return rule;
    }

    static String transform(String pattern, String name) {
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
                    groups[i] = new Long((String)groups[i]);
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

    private static RuntimeException throwUncheked(Throwable e) {
        ViManager2.<RuntimeException>throwAny(e);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }

    private class ManagedNode implements ViNode, SoloNode {

        private String name;
        private ViNodeConfig config = new ViNodeConfig();
        private ViNodeCore realNode;
        private FutureTask<Void> initTask;
        private FutureBox<DirectRemoteExecutor> activeNode = new FutureBox<>();
        private ExecFacade execFacade = new ExecFacade(getExecutor());
        private boolean terminated;

        public ManagedNode(String name) {
            this.name = name;
        }

        @Override
        public DirectRemoteExecutor getExecutor() {
            if (activeNode.isDone()) {
                try {
                    return activeNode.get();
                } catch (InterruptedException | ExecutionException e) {
                    if (e instanceof ExecutionException) {
                        ExceptionHelper.throwUnchecked(e.getCause());
                    }
                    if (e instanceof InterruptedException) {
                        throw new NodeExecutionException(e);
                    }
                    ExceptionHelper.throwUnchecked(e);
                    throw new Error("Unreachable");
                }
            } else {
                return new DirectRemoteExecutor.DeferedDirectRemoteExecutor(activeNode) {

                    @Override
                    public void exec(Runnable task) throws Exception {
                        ensureStarting();
                        super.exec(task);
                    }

                    @Override
                    public <V> V exec(Callable<V> task) throws Exception {
                        ensureStarting();
                        return super.exec(task);
                    }

                    @Override
                    public FutureEx<Void> submit(Runnable task) {
                        ensureStarting();
                        return super.submit(task);
                    }

                    @Override
                    public <V> FutureEx<V> submit(Callable<V> task) {
                        ensureStarting();
                        return super.submit(task);
                    }
                };
            }
        }

        @Override
        public <X> X x(ViNodeExtender<X> extention) {
            return extention.wrap(this);
        }

        @Override
        public <X> X x(ViConfExtender<X> extention) {
            return extention.wrap(this);
        }

        @Override
        public void setProp(String propName, String value) {
            ensureAlive();
            config.setProp(propName, value);
            if (realNode != null) {
                realNode.setProp(propName, value);
            }
        }

        @Override
        public void setProps(Map<String, String> props) {
            if (props.isEmpty()) {
                return;
            }
            ensureAlive();
            config.setProps(props);
            if (realNode != null) {
                realNode.setProps(props);
            }
        }

        @Override
        public Object getPragma(String pragmaName) {
            ensureStarting();
            try {
                activeNode.get();
            } catch (InterruptedException e) {
                throw new NodeExecutionException("Operation interrupted");
            } catch (ExecutionException e) {
                throwUncheked(e.getCause());
            }
            if (realNode != null) {
                return realNode.getPragma(pragmaName);
            }
            else {
                return null;
            }
        }

        @Override
        public void setConfigElement(String key, Object value) {
            ensureAlive();
            this.config.setConfigElement(key, value);
            if (realNode != null) {
                realNode.setConfigElement(key, value);
            }
        }

        @Override
        public void setConfigElements(Map<String, Object> config) {
            if (config.isEmpty()) {
                return;
            }
            ensureAlive();
            this.config.setConfigElements(config);
            if (realNode != null) {
                realNode.setConfigElements(config);
            }
        }

        @Override
        public void touch() {
            exec(new Touch());
        }

        @Override
        public void exec(CloudRunnable task) {
            ensureStarting();
            execFacade.exec(task);
        }

        @Override
        public <V> V calc(CloudCallable<V> task) {
            ensureStarting();
            return execFacade.calc(task);
        }

        @Override
        public CompletableFuture<Void> asyncExec(CloudRunnable task) {
            ensureStarting();
            return execFacade.asyncExec(task);
        }


        @Override
        public <V> CompletableFuture<V> asyncCalc(CloudCallable<V> task) {
            ensureStarting();
            return execFacade.asyncCalc(task);
        }

        @Override
        public MassResult<Void> massExec(CloudRunnable task) {
            ensureStarting();
            return execFacade.massExec(task);
        }


        @Override
        public <V> MassResult<V> massCalc(CloudCallable<V> task) {
            ensureStarting();
            return execFacade.massCalc(task);
        }

        @Override
        public synchronized String getProp(String propName) {
            if (realNode != null) {
                return  realNode.getProp(propName);
            }
            else {
                return config.getProp(propName);
            }
        }

        @Override
        public synchronized void shutdown() {
            if (!terminated) {
                if (realNode != null) {
                    realNode.shutdown();
                    realNode = null;
                }

                terminated = true;
                ViManager2.this.markAsDead(this);
            }
        }

        @Override
        public synchronized void kill() {
            if (!terminated) {
                if (realNode != null) {
                    realNode.kill();
                    realNode = null;
                }

                terminated = true;
                ViManager2.this.markAsDead(this);
            }
        }

        private synchronized void ensureStarting() {
            if (terminated) {
                throw new IllegalStateException("ViNode[" + name + "] is terminated");
            }
            if (initTask == null) {
                LOGGER.debug("ViNode[" + name + "] instantiating");
                initTask = new FutureTask<Void>(new InitTask(), null);
                asyncInitThreads.execute(initTask);
            }
        }

        private synchronized void ensureAlive() {
            if (terminated) {
                throw new IllegalStateException("ViNode[" + name + "] is terminated");
            }
        }

        @Override
        public String toString() {
            return name;
        }

        private ViNodeCore createNode() {
            if (ViProps.NODE_TYPE_ALIAS.equals(config.getProp(ViProps.NODE_TYPE))) {
                String host = config.getProp(ViProps.HOST);
                if (host == null) {
                    throw new IllegalArgumentException("No host specified for node '" + name + "'");
                }
                if (host.startsWith("~")) {
                    host = transform(host, name);
                }
                ViNodeCore hostnode = ((ManagedNode)node(host)).realNode;
                return new ProxyViNode(name, hostnode);
            }
            else {
                ViNodeCore realNode = provider.createNode(name, config);
                return realNode;
            }
        }

        private final class InitTask implements Runnable {
            @Override
            public void run() {
                String tname = swapThreadName("ViNode[" + name + "] init");
                try {
                    try {
                        ViNodeCore realNode = createNode();
                        synchronized(ManagedNode.this) {
                            if (terminated) {
                                activeNode.setError(new RuntimeException("Node terminated"));
                                realNode.shutdown();
                            }
                            else {
                                try {
                                    ManagedNode.this.realNode = realNode;
                                    RemoteExecutor rexec = realNode.executor().exec(RemoteExecutor.INLINE_EXECUTOR_PRODUCER);
                                    if (RemoteStub.isRemoteStub(rexec)) {
                                        activeNode.setData(new ExceptionRefiningNodeExecutor(new RemoteNodeExecutor(rexec), realNode));
                                    }
                                    else {
                                        // fall back, added for compatibility with in-process node type
                                        activeNode.setData(new ExceptionRefiningNodeExecutor(realNode.executor(), realNode));
                                    }
                                    LOGGER.debug("ViNode[" + name + "] instantiated");
                                } catch (Exception e) {
                                    activeNode.setError(e);
                                }
                            }
                        }
                    }
                    catch(Throwable e) {
                        LOGGER.error("ViNode[" + name + "] initialization has failed", e);
                        activeNode.setError(e);
                    }
                }
                finally {
                    swapThreadName(tname);
                }
            }
        }
    }

    private static class Rule implements Comparable<Rule> {

        private final long ruleNo;
        private final NodeSelector selector;
        private final ViNodeConfig config = new ViNodeConfig();

        public Rule(long ruleNo, NodeSelector selector) {
            this.ruleNo = ruleNo;
            this.selector = selector;
        }

        public ViNodeConfig getConfig() {
            return config;
        }

        @Override
        public int compareTo(Rule o) {
            return Long.signum(ruleNo - o.ruleNo);
        }

        @Override
        public String toString() {
            return "[" + ruleNo + "] " + selector.pattern + " -> " + config.toString();
        }
    }

    private class NodeSelector implements ViNode {

        private String pattern;
        private Pattern regEx;

        private List<Rule> rules = new ArrayList<Rule>();

        public NodeSelector(String pattern) {
            this.pattern = pattern;
            this.regEx = GlobHelper.translate(pattern, ".");
        }

        @Override
        public <X> X x(ViNodeExtender<X> extention) {
            return extention.wrap(this);
        }

        @Override
        public <X> X x(ViConfExtender<X> extention) {
            return extention.wrap(this);
        }

        public boolean match(ManagedNode node) {
            return ViManager2.match(regEx, node);
        }

        private ViNode select() {
            return MultiNode.group(listNodes(regEx));
        }

        private ViNodeConfig rule() {
            Rule rule = newRule(this);
            rules.add(rule);
            return rule.getConfig();
        }

        @Override
        public void setProp(String propName, String value) {
            synchronized(ViManager2.this) {
                rule().setProp(propName, value);
                select().setProp(propName, value);
            }
        }

        @Override
        public void setProps(Map<String, String> props) {
            synchronized(ViManager2.this) {
                rule().setProps(props);
                select().setProps(props);
            }
        }

        @Override
        public void setConfigElement(String key, Object value) {
            synchronized(ViManager2.this) {
                rule().setConfigElement(key, value);
                select().setConfigElement(key, value);
            }
        }

        @Override
        public void setConfigElements(Map<String, Object> config) {
            synchronized(ViManager2.this) {
                rule().setConfigElements(config);
                select().setConfigElements(config);
            }
        }

        @Override
        public void touch() {
            select().touch();
        }

        @Override
        public void exec(CloudRunnable task) {
            select().exec(task);
        }

        @Override
        public <V> V calc(CloudCallable<V> task) {
            return select().calc(task);
        }

        @Override
        public CompletableFuture<Void> asyncExec(CloudRunnable task) {
            return select().asyncExec(task);
        }

        @Override
        public <V> CompletableFuture<V> asyncCalc(CloudCallable<V> task) {
            return select().asyncCalc(task);
        }

        @Override
        public MassResult<Void> massExec(CloudRunnable task) {
            return select().massExec(task);
        }

        @Override
        public <V> MassResult<V> massCalc(CloudCallable<V> task) {
            return select().massCalc(task);
        }

        @Override
        public String getProp(String propName) {
            throw new UnsupportedOperationException("Cannot call on group of nodes");
        }

        @Override
        public Object getPragma(String pragmaName) {
            throw new UnsupportedOperationException("Cannot call on group of nodes");
        }

        @Override
        public void kill() {
            select().kill();
        }

        @Override
        public void shutdown() {
            select().shutdown();
        }

        @Override
        public String toString() {
            return pattern;
        }
    }

    private static String swapThreadName(String newName) {
        Thread currentThread = Thread.currentThread();
        String name = currentThread.getName();
        currentThread.setName(newName);
        return name;
    }


    private static class ProxyViNode implements ViNodeCore {

        private final String name;
        private final ViNodeCore node;

        public ProxyViNode(String name, ViNodeCore node) {
            this.name = name;
            this.node = node;
        }

        @Override
        public <X> X x(ViNodeExtender<X> extention) {
            return extention.wrap(this);
        }

        @Override
        public <X> X x(ViConfExtender<X> extention) {
            return extention.wrap(this);
        }

        @Override
        public void touch() {
            node.touch();
        }

        @Override
        public DirectRemoteExecutor executor() {
            return node.executor();
        }

        @Override
        public void setProp(String propName, String value) {
            node.setProp(propName, value);
        }

        @Override
        public void setProps(Map<String, String> props) {
            node.setProps(props);
        }


        @Override
        public void setConfigElement(String key, Object value) {
            node.setConfigElement(key, value);
        }

        @Override
        public void setConfigElements(Map<String, Object> config) {
            node.setConfigElements(config);
        }

        @Override
        public String getProp(String propName) {
            return node.getProp(propName);
        }

        @Override
        public Object getPragma(String pragmaName) {
            return node.getPragma(pragmaName);
        }

        @Override
        public void kill() {
            // do nothing
        }

        @Override
        public void shutdown() {
            // do nothing
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class Touch implements CloudRunnable {

        private static final long serialVersionUID = 20121116L;

        @Override
        public void run() {
            // do nothing
        }
    }

    private static class ExceptionRefiningNodeExecutor implements DirectRemoteExecutor {

        private final DirectRemoteExecutor nested;

        private final ViNodeCore node;

        public ExceptionRefiningNodeExecutor(DirectRemoteExecutor nested, ViNodeCore node) {
            this.nested = nested;
            this.node = node;
        }

        @Override
        public void exec(Runnable task) throws Exception {
            try {
                nested.exec(task);
            } catch (Error e) {
                throw e; // do not mess with errors
            } catch (Throwable e) {
                throwUncheked(refine(e));
            }
        }

        @Override
        public <T> T exec(Callable<T> task) throws Exception {
            try {
                return nested.exec(task);
            } catch (Error e) {
                throw e; // do not mess with errors
            } catch (Throwable e) {
                throw throwUncheked(refine(e));
            }
        }

        @Override
        public FutureEx<Void> submit(Runnable task) {
            try {
                return new FutureWrapper<Void>(nested.submit(task));
            } catch (Error e) {
                throw e; // do not mess with errors
            } catch (Throwable e) {
                throw throwUncheked(refine(e));
            }
        }

        @Override
        public <V> FutureEx<V> submit(Callable<V> task) {
            try {
                return new FutureWrapper<V>(nested.submit(task));
            } catch (Error e) {
                throw e; // do not mess with errors
            } catch (Throwable e) {
                throw throwUncheked(refine(e));
            }
        }

        private Throwable refine(Throwable e) {
            try {
                if (e.toString().startsWith("java.rmi.RemoteException: Connection closed")) {
                    @SuppressWarnings("unchecked")
                    FutureEx<Integer> exitCode = (FutureEx<Integer>) node.x(VX.RUNTIME).exitCodeFuture();
                    if (exitCode.isDone()) {
                        try {
                            Integer code = exitCode.get();
                            RemoteException re = new RemoteException("Connection closed; Remote process terminated, exitCode=" + code, e);
                            return re;
                        } catch (ExecutionException ee) {
                            //ExceptionHelper.replaceCause(e, ee.getCause());
                            // ignore
                        }
                    }
                }
            } catch (Exception ee) {
                // ignore
            }
            return e;
        }

        private class FutureWrapper<V> implements FutureEx<V> {

            private final FutureEx<V> future;
            private final FutureBox<V> box = new FutureBox<V>();

            public FutureWrapper(FutureEx<V> future) {
                this.future = future;
                this.future.addListener(new Box<V>() {

                    @Override
                    public void setData(V data) {
                        box.setData(data);
                    }

                    @Override
                    public void setError(Throwable e) {
                        box.setError(refine(e));
                    }
                });
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                try {
                    return future.cancel(mayInterruptIfRunning);
                } finally {
                    try {
                        // give box a chance to synchronise
                        box.get(1000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            @Override
            public boolean isCancelled() {
                return box.isCancelled();
            }

            @Override
            public boolean isDone() {
                return box.isCancelled();
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return box.get();
            }

            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return box.get(timeout, unit);
            }

            @Override
            public void addListener(Box<? super V> box) {
                this.box.addListener(box);
            }
        }
    }

    private static class RemoteNodeExecutor extends RemoteExecutorAsynAdapter {

        public RemoteNodeExecutor(RemoteExecutor executor) {
            super(executor);
        }
    }
}
