/**
 * Copyright 2014 Alexey Ragozin
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
package org.gridkit.zerormi;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;

/**
 * This is an adapter for {@link RemoteExecutor} exploiting
 * native async method invocation of ZeroRMI.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 */
public class RemoteExecutorAsynAdapter implements DirectRemoteExecutor, AdvancedExecutor {

    private static final Method EXEC_RUNNABLE = getExec(Runnable.class);
    private static final Method EXEC_CALLABLE = getExec(Callable.class);

    private static Method getExec(Class<?> c) {
        try {
            return RemoteExecutor.class.getMethod("exec", c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RemoteExecutor executor;

    public static DirectRemoteExecutor defered(FutureEx<RemoteExecutor> executor) {
        return new DeferedAsyncAdapter(executor);
    }

    public RemoteExecutorAsynAdapter(RemoteExecutor executor) {
        if (!RemoteStub.isRemoteStub(executor)) {
            throw new IllegalArgumentException("Not a remote proxy");
        }
        this.executor = executor;
    }

    @Override
    public void execute(Runnable task) {
        throw new UnsupportedOperationException("This method doesn't make sense for remote execution");
    }

    @Override
    public void exec(Runnable task) throws Exception {
        executor.exec(task);
    }

    @Override
    public <T> T exec(Callable<T> task) throws Exception {
        return executor.exec(task);
    }

    @Override
    public FutureEx<Void> submit(Runnable task) {
        return RemoteStub.remoteSubmit(executor, EXEC_RUNNABLE, task);
    }

    @Override
    public <V> FutureEx<V> submit(Callable<V> task) {
        return RemoteStub.remoteSubmit(executor, EXEC_CALLABLE, task);
    }

    private static class DeferedAsyncAdapter implements DirectRemoteExecutor {

        private final FutureEx<RemoteExecutor> remoteFuture;

        private volatile DirectRemoteExecutor executor;

        protected DeferedAsyncAdapter(FutureEx<RemoteExecutor> remoteFuture) {
            this.remoteFuture = remoteFuture;
            this.remoteFuture.addListener(new Box<RemoteExecutor>() {

                @Override
                public void setData(RemoteExecutor data) {
                    executor = new RemoteExecutorAsynAdapter(data);
                }

                @Override
                public void setError(Throwable e) {
                    // do nothing
                }
            });
        }

        @Override
        public void exec(Runnable task) throws Exception {
            if (executor != null) {
                executor.exec(task);
            } else {
                new RemoteExecutorAsynAdapter(remoteFuture.get()).exec(task);
            }
        }

        @Override
        public <V> V exec(Callable<V> task) throws Exception {
            if (executor != null) {
                return executor.exec(task);
            } else {
                return new RemoteExecutorAsynAdapter(remoteFuture.get()).exec(task);
            }
        }

        @Override
        public FutureEx<Void> submit(final Runnable task) {
            if (executor != null) {
                return executor.submit(task);
            } else {
                final FutureBox<Void> box = new FutureBox<>();
                remoteFuture.addListener(new Box<RemoteExecutor>() {

                    @Override
                    public void setData(RemoteExecutor data) {
                        new RemoteExecutorAsynAdapter(data).submit(task).addListener(box);
                    }

                    @Override
                    public void setError(Throwable e) {
                        box.setErrorIfWaiting(e);
                    }
                });
                return box;
            }
        }

        @Override
        public <V> FutureEx<V> submit(Callable<V> task) {
            if (executor != null) {
                return executor.submit(task);
            } else {
                final FutureBox<V> box = new FutureBox<>();
                remoteFuture.addListener(new Box<RemoteExecutor>() {

                    @Override
                    public void setData(RemoteExecutor data) {
                        new RemoteExecutorAsynAdapter(data).submit(task).addListener(box);
                    }

                    @Override
                    public void setError(Throwable e) {
                        box.setErrorIfWaiting(e);
                    }
                });
                return box;
            }
        }
    }
}
