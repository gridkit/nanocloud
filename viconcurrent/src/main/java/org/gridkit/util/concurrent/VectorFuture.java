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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class VectorFuture<V> implements Future<List<V>> {

	public static <V> VectorFuture<V> lump(Future<V>... futures) {
		return new VectorFuture<V>(Arrays.asList(futures));
	}

	public static <V> VectorFuture<V> lump(Collection<Future<V>> futures) {
		return new VectorFuture<V>(futures);
	}
	
	private Future<V>[] vector;
	private Object[] results;
	private Throwable error;
	private boolean canceled;
	private boolean done;
	
	@SuppressWarnings("unchecked")
	public VectorFuture(Collection<Future<V>> futures) {
		vector = new Future[futures.size()];
		results = new Object[vector.length];
		int n = 0;
		for(Future<V> f : futures) {
			vector[n++] = f;
		}
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone() || isCancelled()) {
			return false;
		}
		boolean result = false;
		for(Future<V> f: vector) {
			if (f != null) {
				result |= f.cancel(mayInterruptIfRunning);
			}
		}
		canceled = true;
		return result;
	}

	@Override
	public synchronized boolean isCancelled() {
		return canceled;
	}

	@Override
	public synchronized boolean isDone() {
		if (done) {
			return true;
		}
		else {
			checkDone();
			return done;
		}
	}

	private synchronized void checkDone() {
		int undone = 0;
		int n = 0;
		for(Future<V> f: vector) {
			if (f != null) {
				if (f.isDone() || f.isCancelled()) {
					collectFuture(n, -1l);
					if (vector[n] != null) {
						++undone;
					}
				}
				else {
					++undone;
				}
			}
			++n;
		}		
		if (undone == 0) {
			done = true;
		}
	}

	private synchronized void collectFuture(int n, long timeout) {
		try {
			V result = timeout < 0 ? vector[n].get() : vector[n].get(timeout, TimeUnit.NANOSECONDS);
			results[n] = result;
			vector[n] = null;
		} catch (TimeoutException e) {
			return;
		} catch (InterruptedException e) {
			Thread.interrupted();
			return;
		} catch (ExecutionException e) {
			if (error == null) {
				if (e.getCause() instanceof Throwable) {
					error = e.getCause();
				}
				else {
					error = e;
				}
			}
			vector[n] = null;
		}
	}

	@Override
	public List<V> get() throws InterruptedException, ExecutionException {
		while(true) {
			try {
				return get(365, TimeUnit.DAYS);
			} catch (TimeoutException e) {
				// ignore
			}
		}
	}

	@Override
	public List<V> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		long to;
		while((to = deadline - System.nanoTime()) > 0) {
			synchronized(this) {
				checkDone();
				if (done || canceled) {
					return getResult();
				}
				else {
					int n = 0;
					for(Future<V> f: vector) {
						if (f != null) {
							collectFuture(n, (to / 2) + 1);
						}
						++n;
					}
				}
			}
		}
		throw new TimeoutException();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<V> getResult() throws ExecutionException {
		if (error != null) {
			throw new ExecutionException(error);
		}
		else {
			return Collections.unmodifiableList((List)Arrays.asList(results));
		}
	}
}
