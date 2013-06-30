package org.gridkit.nanocloud.interceptor;

import java.io.Serializable;
import java.util.concurrent.BrokenBarrierException;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;
import org.gridkit.util.concurrent.BlockingBarrier;

class BarrierStub implements Interceptor, Serializable {

	private static final long serialVersionUID = 20130621L;
	
	private BlockingBarrier barrier;

	public BarrierStub(BlockingBarrier barrier) {
		this.barrier = barrier;
	}

	@Override
	public void handle(Interception call) {
		try {
			barrier.pass();
		} catch (InterruptedException e) {
		} catch (BrokenBarrierException e) {
		}
	}
}
