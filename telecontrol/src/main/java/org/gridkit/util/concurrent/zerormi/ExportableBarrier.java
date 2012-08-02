package org.gridkit.util.concurrent.zerormi;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.concurrent.FutureLatch;

/**
 * ZeroRMI remote adapter for {@link BlockingBarrier}.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com) 
 */
@SuppressWarnings("serial") // ZeroRMI
public class ExportableBarrier implements BlockingBarrier, Serializable {

	public static ExportableBarrier export(BlockingBarrier barrier) {
		return new ExportableBarrier(barrier);
	}
	
	private RemoteBarrier delegate;
	private transient BlockingBarrier localBarrier;
	
	public ExportableBarrier(BlockingBarrier barrier) {
		delegate = new BarrierDelegate(barrier);
	}
	
	@Override
	public void pass() throws InterruptedException, BrokenBarrierException {
		delegate.pass();
	}

	@Override
	public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException {
		delegate.pass(breakOnInterrupt);
	}

	@Override
	public void breakthrough() {
		delegate.breakthrough();		
	}

	@Override
	public void stepIn() {
		delegate.stepIn();		
	}

	@Override
	public Future<Void> stepIn(boolean needFuture) {
		if (needFuture && localBarrier == null) {
			final FutureLatch latch = new FutureLatch();
			delegate.stepIn(new BarrierCallback() {
				@Override
				public void passed() {
					latch.open();
				}
				@Override
				public void broken() {
					latch.open();
				}
			});
			return latch;
		}
		else {
			return localBarrier.stepIn(needFuture);
		}
	}

	private static interface RemoteBarrier extends BlockingBarrier, Remote {
		
		public void stepIn(BarrierCallback callback); 
		
	}
	
	private static interface BarrierCallback extends Remote {
		public void passed();
		public void broken();
	}
	
	private static class BarrierDelegate implements RemoteBarrier {
		
		private BlockingBarrier barrier;
		
		public BarrierDelegate(BlockingBarrier barrier) {
			this.barrier = barrier;
		}

		public void pass() throws InterruptedException, BrokenBarrierException {
			barrier.pass();
		}

		public void pass(boolean breakOnInterrupt) throws InterruptedException, BrokenBarrierException {
			barrier.pass(breakOnInterrupt);
		}

		public void breakthrough() {
			barrier.breakthrough();
		}

		public void stepIn() {
			barrier.stepIn();
		}

		public Future<Void> stepIn(boolean needFuture) {
			if (needFuture) {
				throw new IllegalArgumentException("Future cannot be returned");
			}
			else {
				barrier.stepIn();
				return null;
			}
		}

		@Override
		public void stepIn(final BarrierCallback callback) {
			final Future<?> future = barrier.stepIn(true);
			Runnable trigger = new Runnable() {
				@Override
				public void run() {
					try {
						future.get();
						callback.passed();
					}
					catch(Exception e) {
						callback.broken();
					}
				}
			};
			if (future.isDone()) {
				trigger.run();
			}
			else {
				FutureNotifier.track(future, trigger);
			}			
		}
	}
}
