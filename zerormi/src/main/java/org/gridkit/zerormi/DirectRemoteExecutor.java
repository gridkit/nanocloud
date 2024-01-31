package org.gridkit.zerormi;

import java.util.concurrent.Callable;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;

/**
 * This interface expose direct capabilities of ZeroRMI remote calls.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface DirectRemoteExecutor {

    /**
     * Synchronous call.
     */
    public void exec(Runnable task) throws Exception;

    /**
     * Synchronous call.
     */
    public <V> V exec(Callable<V> task) throws Exception;

    /**
     * Asynchronous call. Future will be bound to return protocol message.
     */
    public FutureEx<Void> submit(Runnable task);

    /**
     * Asynchronous call. Future will be bound to return protocol message.
     */
    public <V> FutureEx<V> submit(Callable<V> task);


    public static class DeferedDirectRemoteExecutor implements DirectRemoteExecutor {

        private final FutureEx<DirectRemoteExecutor> delegate;

        public DeferedDirectRemoteExecutor(FutureEx<DirectRemoteExecutor> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void exec(Runnable task) throws Exception {
            delegate.get().exec(task);
        }

        @Override
        public <V> V exec(Callable<V> task) throws Exception {
            return delegate.get().exec(task);
        }

        @Override
        public FutureEx<Void> submit(final Runnable task) {
            FutureBox<Void> fbox = new FutureBox<>();
            delegate.addListener(new Box<DirectRemoteExecutor>() {

                @Override
                public void setData(DirectRemoteExecutor data) {
                    data.submit(task).addListener(fbox);
                }

                @Override
                public void setError(Throwable e) {
                    fbox.setErrorIfWaiting(e);

                }
            });
            return fbox;
        }

        @Override
        public <V> FutureEx<V> submit(Callable<V> task) {
            FutureBox<V> fbox = new FutureBox<>();
            delegate.addListener(new Box<DirectRemoteExecutor>() {

                @Override
                public void setData(DirectRemoteExecutor data) {
                    data.submit(task).addListener(fbox);
                }

                @Override
                public void setError(Throwable e) {
                    fbox.setErrorIfWaiting(e);

                }
            });
            return fbox;
        }
    }
}
