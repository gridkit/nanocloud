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

import java.io.Serializable;

import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.concurrent.Latch;

/**
 * ZeroRMI remote adapter for {@link BlockingBarrier}.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com) 
 */
@SuppressWarnings("serial") // ZeroRMI
public class ExportableLatchBarrier extends ExportableBarrier implements BlockingBarrier, Latch, Serializable {

	public static ExportableLatchBarrier export(BlockingBarrier barrier) {
		Latch latch = (Latch) barrier;
		return new ExportableLatchBarrier((BlockingBarrier) latch);
	}

	public ExportableLatchBarrier(BlockingBarrier barrier) {
		super(barrier);
	}

	@Override
	public void open() {
	}
}
