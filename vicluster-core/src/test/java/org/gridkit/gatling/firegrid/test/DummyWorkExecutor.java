package org.gridkit.gatling.firegrid.test;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.gatling.firegrid.WorkExecutor;
import org.gridkit.gatling.firegrid.WorkPacket;
import org.gridkit.util.formating.Formats;
import org.junit.Ignore;

@SuppressWarnings("serial") 
@Ignore
public class DummyWorkExecutor implements WorkExecutor, Serializable {

	long packetTimestamp;
	AtomicInteger callCount = new AtomicInteger();
	
	@Override
	public void initialize() {
	}

	@Override
	public void initWorkPacket(WorkPacket packet) {
		packetTimestamp = System.nanoTime();
		callCount.set(0);
	}

	@Override
	public int execute() {
		callCount.incrementAndGet();
		return 0;
	}

	@Override
	public void finishWorkPacket(WorkPacket packet) {
		long time = System.nanoTime() - packetTimestamp;
		double rate = 1d * TimeUnit.SECONDS.toNanos(1) * callCount.get() /time;
		System.out.println(Formats.currentDatestamp() + String.format("] Workpacket finished, rate %.3f op/s", rate));
	}

	@Override
	public void shutdown() {
	}
}