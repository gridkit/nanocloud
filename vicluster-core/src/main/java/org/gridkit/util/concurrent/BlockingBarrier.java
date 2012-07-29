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

public interface BlockingBarrier {
	
	
	/**
	 * Block until it could pass barrier.
	 * {@link InterruptedException} will during wait will make barrier broken. 
	 */
	public void pass() throws InterruptedException, BrokenBarrierException;

	/**
	 * Block until it could pass barrier.
	 * @param breakOnInterrupt - whenever {@link InterruptedException} should switch barrier in broken state. 
	 */
	public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException;
	
	/**
	 * Pass barrier without blocking. If barrier is not open, it will be broken.
	 */
	public void breakthrough();

	/**
	 * Enter barrier by do not wait for passing though.
	 */
	public void stepIn();

	/**
	 * Enter barrier but receive passing {@link Future} instead of blocking.
	 */
	public Future<Void> stepIn(boolean needFuture);

}
