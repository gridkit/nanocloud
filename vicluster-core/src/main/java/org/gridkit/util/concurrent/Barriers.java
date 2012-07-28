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
