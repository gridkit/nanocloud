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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;

/**
 * {@link BlockingBarrier} adapter for {@link CyclicBarrier}.
 * TODO {@link Future} support is missing along with other minor semantic aspects.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CyclicBlockingBarrier implements BlockingBarrier {

	private CyclicBarrier barrier;
	
	public CyclicBlockingBarrier(int parties, Runnable action) {
		barrier = new CyclicBarrier(parties, action);
	}
	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		barrier.await();
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException {
		if (breakOnInterrupt == false) {
			throw new UnsupportedOperationException();
		}
		barrier.await();
	}

	@Override
	public void breakthrough() {
		barrier.reset();
	}

	@Override
	public void stepIn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Void> stepIn(boolean needFuture) {
		throw new UnsupportedOperationException();
	}
}
