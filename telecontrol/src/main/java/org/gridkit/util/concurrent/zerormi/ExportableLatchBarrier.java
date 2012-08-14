package org.gridkit.util.concurrent.zerormi;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.concurrent.FutureLatch;
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
