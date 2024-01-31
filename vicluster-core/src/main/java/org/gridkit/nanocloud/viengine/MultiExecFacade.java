package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.gridkit.nanocloud.ViExecutor;
import org.gridkit.nanocloud.viengine.ExecFacade.CRAdapter;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.DirectRemoteExecutor;

class MultiExecFacade implements ViExecutor {

    private List<DirectRemoteExecutor> executors = new ArrayList<DirectRemoteExecutor>();

    protected MultiExecFacade(List<DirectRemoteExecutor> executors) {
        this.executors = executors;
    }

    @Override
    public void exec(CloudRunnable task) {
        MassResult<Void> mr = massExec(task);
        mr.all();
        return;
    }

    @Override
    public <V> V calc(CloudCallable<V> task) {
        MassResult<V> mr = massCalc(task);
        return mr.all().iterator().next();
    }

    @Override
    public CompletableFuture<Void> asyncExec(CloudRunnable task) {
        MassResult<Void> mr = massExec(task);
        return mr.allFuture().thenApply(c -> null);
    }

    @Override
    public <V> CompletableFuture<V> asyncCalc(CloudCallable<V> task) {
        MassResult<V> mr = massCalc(task);
        return mr.allFuture().thenApply(c -> c.iterator().next());
    }

    @Override
    public MassResult<Void> massExec(CloudRunnable task) {
        CRAdapter crtask = new CRAdapter(task);
        List<FutureEx<Void>> futures = new ArrayList<>();
        for (DirectRemoteExecutor exec: executors) {
            futures.add(exec.submit(crtask));
        }
        MassResultImpl<Void> mr = new MassResultImpl<>(futures);
        return mr;
    }

    @Override
    public <V> MassResult<V> massCalc(CloudCallable<V> task) {
        List<FutureEx<V>> futures = new ArrayList<>();
        for (DirectRemoteExecutor exec: executors) {
            futures.add(exec.submit(task));
        }
        MassResultImpl<V> mr = new MassResultImpl<>(futures);
        return mr;
    }
}
