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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class FutureBox<V> implements FutureEx<V>, Box<V> {


	private static boolean STOPPABLE = false;
	private static final long STOPPABLE_WAIT_INTERVAL = TimeUnit.MILLISECONDS.toNanos(500);
	
	/**
	 * This is a hack to make JUnit timeouts, relaying on {@link Thread#stop()} work some how.
	 */
	static void enableStoppability(boolean stoppability) {
		STOPPABLE = stoppability;
	}
	
	public static <T> FutureEx<T> dataFuture(T data) {
		FutureBox<T> fb = new FutureBox<T>();
		fb.setData(data);
		return fb;
	}

	public static <T> FutureEx<T> errorFuture(Exception e) {
		FutureBox<T> fb = new FutureBox<T>();
		fb.setError(e);
		return fb;
	}
	
	private final FutureTask<V> ft = new FutureTask<V>(new Callable<V>() {
		@Override
		public V call() throws Exception {
			synchronized(FutureBox.this) {
				if (!finalized) {
					throw new Error("Unexpected call time");
				}
				else {
					if (error != null) {
						AnyThrow.throwUncheked(error);
						throw new Error("Unreachable");
					}
					else {
						return value;
					}
				}
			}
		}
	});
	
	private boolean finalized;
	private V value;
	private Throwable error;
	private List<Box<? super V>> triggers;

	@Override
	public synchronized void setData(V data) {
		if (finalized) {
			throw new IllegalStateException("Box is closed");
		}
		else {
			finalized = true;
			value = data;
			ft.run();
			notifyTriggers();
		}		
	}

	@Override
	public synchronized void setError(Throwable e) {
		if (finalized) {
			throw new IllegalStateException("Box is closed");
		}
		else {
			finalized = true;
			error = e;
			ft.run();
			notifyTriggers();
		}		
	}

	public synchronized void setErrorIfWaiting(Throwable e) {
		if (finalized) {
			return;
		}
		else {
			finalized = true;
			error = e;
			ft.run();
			notifyTriggers();
		}		
	}

	private void notifyTriggers() {
		if (triggers != null) {
			for(Box<? super V> r: triggers) {
				try {
					pushToBox(r);
				}
				catch(Exception e) {
					Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
				}
			}
			triggers = null;
		}
	}

	private void pushToBox(Box<? super V> r) {
		if (error != null) {
			r.setError(error);
		}
		else {
			r.setData(value);
		}
	}

	
	@Override
	public synchronized void addListener(Box<? super V> box) {
		if (finalized) {
			pushToBox(box);
		}
		else {
			if (triggers == null) {
				triggers = new ArrayList<Box<? super V>>();
			}
			triggers.add(box);
		}		
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return ft.cancel(mayInterruptIfRunning);
	}
	
	@Override
	public boolean isCancelled() {
		return ft.isCancelled();
	}
	
	@Override
	public boolean isDone() {
		return ft.isDone();
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		if (STOPPABLE) {
			try {
				return getStoppable(-1, TimeUnit.NANOSECONDS);
			} catch (TimeoutException e) {
				throw new Error("Should never happen");
			}
		}
		else {
			return ft.get();
		}
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (STOPPABLE) {
			return getStoppable(timeout, unit);
		}
		else {
			return ft.get(timeout, unit);
		}
	}

	private V getStoppable(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long deadline = timeout == -1 ? Long.MAX_VALUE : System.nanoTime() + unit.toNanos(timeout);
		while(true) {
			long to = deadline - System.nanoTime();
			if (to < 0) {
				to = 0;
			}
			else if (to > STOPPABLE_WAIT_INTERVAL) {
				to = STOPPABLE_WAIT_INTERVAL;				
			}
			try {
				return ft.get(to, TimeUnit.NANOSECONDS);
			}
			catch(TimeoutException e) {
				if (deadline < System.nanoTime()) {
					throw e;
				}
				else {
					continue;
				}
			}
		}
	}
	
	private static class AnyThrow {

	    public static void throwUncheked(Throwable e) {
	        AnyThrow.<RuntimeException>throwAny(e);
	    }
	   
	    @SuppressWarnings("unchecked")
	    private static <E extends Throwable> void throwAny(Throwable e) throws E {
	        throw (E)e;
	    }
	}	
}
