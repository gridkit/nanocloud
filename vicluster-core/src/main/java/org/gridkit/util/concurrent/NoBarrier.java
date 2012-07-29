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
import java.util.concurrent.Future;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NoBarrier implements BlockingBarrier {

	public static final NoBarrier INSTANCE = new NoBarrier();

	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		// do nothing
		
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException {
		// do nothing		
	}

	@Override
	public void breakthrough() {
		// do nothing		
	}

	@Override
	public void stepIn() {
		// do nothing
	}
	
	@Override
	public Future<Void> stepIn(boolean needFuture) {
		Future<Void> result = null;
		if (needFuture) {
			FutureLatch latch = new FutureLatch();
			latch.open();
			result = latch;
		}
		return result;
	}
}
