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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * Simple latch barrier. Latch is created in closed state and could be opened just ones.
 * 
 * This kind of barrier does not handles broken state.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class LatchBarrier implements BlockingBarrier, Latch {

	private CountDownLatch latch = new CountDownLatch(1);
	
	@Override
	public void open() {
		latch.countDown();
	}
	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		pass(true);
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException {
		latch.await();
	}

	@Override
	public void breakthrough() {
		// do nothing
	}

	@Override
	public void stepIn() {
		stepIn(false);
	}
	
	@Override
	public Future<Void> stepIn(boolean needFuture) {
		return needFuture ? new FutureLatch(latch) : null;
	}
}
