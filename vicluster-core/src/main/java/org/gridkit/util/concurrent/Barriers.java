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

public class Barriers {

	public static NoBarrier openBarrier() {
		return NoBarrier.INSTANCE;
	}
	
	public static BlockingBarrier speedLimit(double eventsPerSecond) {
		if (eventsPerSecond < 0) {
			throw new IllegalArgumentException("speedLimit should be >= 0");
		}
		else if (eventsPerSecond == Double.POSITIVE_INFINITY) {
			return openBarrier();
		}
		else {
			int replenishLimit = (int)(eventsPerSecond * 0.1);
			if (replenishLimit < 1) {
				replenishLimit = 1;
			}
			return new SimpleSpeedLimit(eventsPerSecond, replenishLimit);
		}
	}
	
}
