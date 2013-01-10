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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple future which becomes "complete" by timer.
 * Cannot be canceled.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class TimedFuture implements Future<Void> {

	public static Future<Void> delay(long delay, TimeUnit tu) {
		return new TimedFuture(System.nanoTime() + tu.toNanos(delay));
	}
	
	private static long ANCHOR = System.nanoTime();
	
	private final long nanodeadline;
	
	public TimedFuture(long nanodeadline) {
		this.nanodeadline = nanodeadline - ANCHOR;		
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return (System.nanoTime() - ANCHOR) > nanodeadline;
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException {
		await(Long.MAX_VALUE >> 1, TimeUnit.NANOSECONDS);
		return null;
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		await(timeout,unit);
		if (!isDone()) {
			throw new TimeoutException();
		}
		return null;
	}

	private void await(long timeout, TimeUnit unit) throws InterruptedException {
		long waitdeadline = System.nanoTime() - ANCHOR + unit.toNanos(timeout);
		if (waitdeadline > nanodeadline) {
			waitdeadline = nanodeadline;
		}
		while(true) {
			long sleep = waitdeadline - (System.nanoTime() - ANCHOR);
			if (sleep <=0) {
				break;
			}
			else {
				Thread.sleep(TimeUnit.NANOSECONDS.toMillis(sleep));
			}
		}
	}
}
