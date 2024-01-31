package org.gridkit.nanocloud.viengine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViExecutor;
import org.gridkit.nanocloud.ViNode;
import org.gridkit.nanocloud.ViNodeExtender;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.zerormi.DirectRemoteExecutor;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class MultiNode implements ViNode {

    public static ViNode group(ViNode... hosts) {
        return group(Arrays.asList(hosts));
    }

    public static ViNode group(Collection<? extends ViNode> hosts) {
        if (hosts.size() == 1) {
            return hosts.iterator().next();
        }
        MultiNode group = new MultiNode();
        for(ViNode host: hosts) {
            if (host instanceof MultiNode) {
                for (ViNode hh: ((MultiNode) host).hosts) {
                    group.addNode(hh);
                }
            } else {
                group.addNode(host);
            }
        }
        group.init();
        return group;
    }

    private ViNodeConfig config = new ViNodeConfig();
    private List<ViNode> hosts = new ArrayList<ViNode>();
    private ViExecutor execFacade;
    private boolean shutdown = false;

    private void init() {
        List<DirectRemoteExecutor> executors = new ArrayList<>();
        for (ViNode host: hosts) {
            executors.add(((SoloNode)host).getExecutor());
        }
        if (hosts.isEmpty()) {
            execFacade = new EmptyFacade();
        } else {
            execFacade = new MultiExecFacade(executors);
        }
    }

    private void checkActive() {
        if (shutdown) {
            throw new IllegalStateException("Group is shutdown");
        }
    }

    private void checkExecutable() {
        checkActive();
        if (hosts.isEmpty()) {
            throw new IllegalStateException("No hosts in this group");
        }
    }

    public synchronized void addNode(ViNode host) {
        if (host == null) {
            throw new NullPointerException("null ViNode reference");
        }
        checkActive();
        hosts.add(host);
        config.apply(host);
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
    public synchronized void setProp(String propName, String value) {
        checkActive();
        config.setProp(propName, value);
        for(ViNode vh: hosts) {
            vh.setProp(propName, value);
        }
    }

    @Override
    public synchronized void setProps(Map<String, String> props) {
        checkActive();
        config.setProps(props);
        for(ViNode vh: hosts) {
            vh.setProps(props);
        }
    }

    @Override
    public String getProp(String propName) {
        throw new UnsupportedOperationException("Unsupported for group");
    }

    @Override
    public String getPragma(String pragmaName) {
        throw new UnsupportedOperationException("Unsupported for group");
    }

    @Override
    public void setConfigElement(String key, Object value) {
        checkActive();
        config.setConfigElement(key, value);
        for(ViNode vh: hosts) {
            vh.setConfigElement(key, value);
        }
    }

    @Override
    public void setConfigElements(Map<String, Object> config) {
        checkActive();
        this.config.setConfigElements(config);
        for(ViNode vh: hosts) {
            vh.setConfigElements(config);
        }
    }

    @Override
    public void kill() {
        if (!shutdown) {
            for(ViNode host: hosts) {
                host.kill();
            }
            shutdown = true;
        }
    }

    @Override
    public synchronized void shutdown() {
        if (!shutdown) {
            for(ViNode host: hosts) {
                host.shutdown();
            }
            shutdown = true;
        }
    }



    @Override
    public void exec(CloudRunnable task) {
        checkExecutable();
        execFacade.exec(task);
    }

    @Override
    public <V> V calc(CloudCallable<V> task) {
        checkExecutable();
        return execFacade.calc(task);
    }

    @Override
    public CompletableFuture<Void> asyncExec(CloudRunnable task) {
        checkExecutable();
        return execFacade.asyncExec(task);
    }

    @Override
    public <V> CompletableFuture<V> asyncCalc(CloudCallable<V> task) {
        checkExecutable();
        return execFacade.asyncCalc(task);
    }

    @Override
    public MassResult<Void> massExec(CloudRunnable task) {
        checkExecutable();
        return execFacade.massExec(task);
    }

    @Override
    public <V> MassResult<V> massCalc(CloudCallable<V> task) {
        checkExecutable();
        return execFacade.massCalc(task);
    }

    @Override
    public void touch() {
        exec(new Touch());
    }

    private static class EmptyFacade implements ViExecutor {

        @Override
        public void exec(CloudRunnable task) {
            throw new IllegalArgumentException("No nodes selected");
        }

        @Override
        public <V> V calc(CloudCallable<V> task) {
            throw new IllegalArgumentException("No nodes selected");
        }

        @Override
        public CompletableFuture<Void> asyncExec(CloudRunnable task) {
            throw new IllegalArgumentException("No nodes selected");
        }

        @Override
        public <V> CompletableFuture<V> asyncCalc(CloudCallable<V> task) {
            throw new IllegalArgumentException("No nodes selected");
        }

        @Override
        public MassResult<Void> massExec(CloudRunnable task) {
            throw new IllegalArgumentException("No nodes selected");
        }

        @Override
        public <V> MassResult<V> massCalc(CloudCallable<V> task) {
            throw new IllegalArgumentException("No nodes selected");
        }
    }

    private static class Touch implements CloudRunnable, Serializable {

        private static final long serialVersionUID = 20121116L;

        @Override
        public void run() {
        }
    }
}
