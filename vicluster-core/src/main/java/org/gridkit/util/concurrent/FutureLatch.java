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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class FutureLatch implements Future<Void>, Latch {
	
	private CountDownLatch latch = new CountDownLatch(1);

	public FutureLatch() {
		latch = new CountDownLatch(1);
	}

	public FutureLatch(CountDownLatch latch) {
		this.latch = latch;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone()) {
			return false;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return latch.getCount() <= 0;
	}

	public void open() {
		latch.countDown();
	}
	
	@Override
	public Void get() throws InterruptedException, ExecutionException {
		latch.await();
		return null;
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (latch.await(timeout, unit)) {
			return null;
		}
		else {
			throw new TimeoutException();
		}
	}
}
