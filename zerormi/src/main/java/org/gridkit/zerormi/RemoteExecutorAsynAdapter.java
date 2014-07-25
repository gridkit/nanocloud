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
import org.gridkit.util.concurrent.FutureEx;

/**
 * This is an adapter for {@link RemoteExecutor} exploiting
 * native async method invocation of ZeroRMI.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 */
public class RemoteExecutorAsynAdapter implements AdvancedExecutor {

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
    
    public void exec(Runnable task) throws Exception {
        executor.exec(task);
    }
    
    public <T> T exec(Callable<T> task) throws Exception {
        return executor.exec(task);        
    }
    
    public FutureEx<Void> submit(Runnable task) {
        return RemoteStub.remoteSubmit(executor, EXEC_RUNNABLE, task);
    }

    public <V> FutureEx<V> submit(Callable<V> task) {
        return RemoteStub.remoteSubmit(executor, EXEC_CALLABLE, task);
    }    
}
