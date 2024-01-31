package org.gridkit.nanocloud;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ViExecutor {

    public void exec(CloudRunnable task);

    public default void execRunnable(Runnable task) {
        exec(task::run);
    }

    public <V> V calc(CloudCallable<V> task);

    public default <V> V calcCallable(Callable<V> task) {
        return calc(task::call);
    }

    public CompletableFuture<Void> asyncExec(CloudRunnable task);

    public default CompletableFuture<Void> asyncExecRunnable(Runnable task) {
        return asyncExec(task::run);
    }

    public <V> CompletableFuture<V> asyncCalc(CloudCallable<V> task);

    public default <V> CompletableFuture<V> asyncCalcCallable(Callable<V> task) {
        return asyncCalc(task::call);
    }

    public MassResult<Void> massExec(CloudRunnable task);

    public default MassResult<Void> massExecRunnable(Runnable task) {
        return massExec(task::run);
    }

    public <V> MassResult<V> massCalc(CloudCallable<V> task);

    public default <V> MassResult<V> massCalcCallable(Callable<V> task) {
        return massCalc(task::call);
    }

    public interface MassResult<V> {

        public V first();

        public Collection<V> all();

        public Iterable<V> results();

        public int size();

        public CompletableFuture<V> firstFuture();

        public CompletableFuture<Collection<V>> allFuture();

    }

    public interface CloudRunnable extends Serializable {

        public void run() throws Exception;

    }

    public interface CloudCallable<V> extends Callable<V>, Serializable {

        @Override
        public V call() throws Exception;

    }
}
