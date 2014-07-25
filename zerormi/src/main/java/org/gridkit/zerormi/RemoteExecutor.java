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

import java.io.Serializable;
import java.rmi.Remote;
import java.util.concurrent.Callable;

/**
 * This is minimal task execution interface for transparent remoting.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface RemoteExecutor extends Remote {

    public static final Callable<RemoteExecutor> INLINE_EXECUTOR_PRODUCER = new InlineRemoteExecutorProducer();
    
    public void exec(Runnable task) throws Exception;

    public <T> T exec(Callable<T> task) throws Exception;
   
    public static class InlineRemoteExecutor implements RemoteExecutor {

        @Override
        public void exec(Runnable task) throws Exception {
            task.run();
        }

        @Override
        public <T> T exec(Callable<T> task) throws Exception {
            return task.call();
        }
    }

    /**
     * Due to complications of isolate classloaders run mode
     * this callable should be in this package.
     */
    public static class InlineRemoteExecutorProducer implements Callable<RemoteExecutor>, Serializable {

        private static final long serialVersionUID = 20140725L;

        @Override
        public RemoteExecutor call() throws Exception {
            return new InlineRemoteExecutor();
        }
    }
}
