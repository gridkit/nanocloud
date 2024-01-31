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
package org.gridkit.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;


public interface AdvancedExecutor extends Executor {

    @Override
    public void execute(Runnable task);

    public FutureEx<Void> submit(Runnable task);

    public <V> FutureEx<V> submit(Callable<V> task);

    public interface Component extends AdvancedExecutor {

        public void shutdown();

    }
}
