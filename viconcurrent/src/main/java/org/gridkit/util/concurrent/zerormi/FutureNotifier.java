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
package org.gridkit.util.concurrent.zerormi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class FutureNotifier {

	public static void track(Future<?> future, Runnable callback) {
		Lazy.INSTANCE.internalTrack(future, callback);
	}
	
	private static class Lazy {
		
		private static FutureNotifier INSTANCE = new FutureNotifier();
		
	}
	
	private ExecutorService executor;
	
	private FutureNotifier() {
		executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("FutureNotifier-idle");
				return t;
			}
		});
	}
	
	private void internalTrack(final Future<?> future, final Runnable callback) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				String threadName = Thread.currentThread().getName();
				Thread.currentThread().setName("FutureNotifier-" + future);
				try {
					try {
						future.get();
					}
					catch(Exception e) {
						// ignore;
					}
					callback.run();
				} 
				finally {
					Thread.currentThread().setName(threadName);
				}
			}
		});
	}
}
