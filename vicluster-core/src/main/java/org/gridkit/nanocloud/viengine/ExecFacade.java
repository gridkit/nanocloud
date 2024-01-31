package org.gridkit.nanocloud.viengine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.gridkit.nanocloud.ViExecutor;
import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.zerormi.DirectRemoteExecutor;

class ExecFacade implements ViExecutor {

    private final DirectRemoteExecutor nodeExecutor;

    protected ExecFacade(DirectRemoteExecutor nodeExecutor) {
        super();
        this.nodeExecutor = nodeExecutor;
    }

    public DirectRemoteExecutor getDirectExecutor() {
        return nodeExecutor;
    }

    @Override
    public void exec(CloudRunnable task) {
        AdvExecutor2ViExecutor.exec(nodeExecutor, new CRAdapter(task));
    }

    @Override
    public <V> V calc(CloudCallable<V> task) {
        return AdvExecutor2ViExecutor.exec(nodeExecutor, task);
    }

    @Override
    public CompletableFuture<Void> asyncExec(CloudRunnable task) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        Completer<Void> cc = new Completer<>(cf);
        nodeExecutor.submit(new CRAdapter(task)).addListener(cc);
        return cf;
    }

    @Override
    public <V> CompletableFuture<V> asyncCalc(CloudCallable<V> task) {
        CompletableFuture<V> cf = new CompletableFuture<>();
        Completer<V> cc = new Completer<>(cf);
        nodeExecutor.submit(task).addListener(cc);
        return cf;
    }

    @Override
    public MassResult<Void> massExec(CloudRunnable task) {
        List<FutureEx<Void>> futures = new ArrayList<>();
        futures.add(nodeExecutor.submit(new CRAdapter(task)));
        return new MassResultImpl<Void>(futures);
    }

    @Override
    public <V> MassResult<V> massCalc(CloudCallable<V> task) {
        List<FutureEx<V>> futures = new ArrayList<>();
        futures.add(nodeExecutor.submit(task));
        return new MassResultImpl<V>(futures);
    }

    private static class Completer<V> implements Box<V> {

        private final CompletableFuture<V> fut;

        protected Completer(CompletableFuture<V> fut) {
            this.fut = fut;
            fut.handle(this::handle); // string ref
        }

        public V handle(V value, Throwable e) {
            if (e != null) {
                ExceptionHelper.throwUnchecked(e);
            }
            return value;
        }

        @Override
        public void setData(V data) {
            fut.complete(data);
        }

        @Override
        public void setError(Throwable e) {
            fut.completeExceptionally(e);
        }
    }

    static class CRAdapter implements Runnable, Serializable {

        private static final long serialVersionUID = 20240129L;

        private final CloudRunnable task;

        protected CRAdapter(CloudRunnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            try {
                task.run();
            } catch (Exception e) {
                ExceptionHelper.throwUnchecked(e);
            }
        }
    }
}
