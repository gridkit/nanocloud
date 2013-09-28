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

public class AdvancedExecutorAdapter implements AdvancedExecutor {
	
	private final Executor executor;

	public AdvancedExecutorAdapter(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void execute(Runnable task) {
		executor.execute(task);		
	}

	@Override
	public FutureEx<Void> submit(final Runnable task) {
		final FutureBox<Void> fb = new FutureBox<Void>();
		execute(new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
					fb.setData(null);
				}
				catch(Throwable e) {
					fb.setError(e);
				}
			}
		});
		return fb;
	}

	@Override
	public <V> FutureEx<V> submit(final Callable<V> task) {
		final FutureBox<V> fb = new FutureBox<V>();
		execute(new Runnable() {
			@Override
			public void run() {
				try {
					fb.setData(task.call());
				}
				catch(Throwable e) {
					fb.setError(e);
				}
			}
		});
		return fb;
	}
}
