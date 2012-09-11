package org.gridkit.zerormi;

import java.util.concurrent.CancellationException;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.util.concurrent.RunnableEx;

public class BlobPipeThreadingAdapter implements DuplexBlobPipe {

	private final DuplexBlobPipe pipe;
	private final AdvancedExecutor downStream;
	private final AdvancedExecutor upStream;
	
	public BlobPipeThreadingAdapter(DuplexBlobPipe pipe, AdvancedExecutor downStream, AdvancedExecutor upstream) {
		this.pipe = pipe;
		this.downStream = downStream;
		this.upStream = upstream;
	}
	
	@Override
	public void bind(BlobReceiver receiver) {
		if (upStream == null) {
			pipe.bind(receiver);
		}
		else {
			pipe.bind(new ReceiverAdapter(receiver, upStream));
		}
	}

	@Override
	public FutureEx<Void> sendBinary(final byte[] bytes) {
		if (downStream == null) {
			return pipe.sendBinary(bytes);
		}
		else {
			final FutureBox<Void> ack = new FutureBox<Void>();
			downStream.submit(new RunnableEx() {
				@Override
				public void run() {
					try {
						pipe.sendBinary(bytes).addListener(ack);
					}
					catch(Exception e) {
						ack.setError(e);
					}
				}
				
				@Override
				public void cancelled() {
					ack.setError(new CancellationException());
				}
			});			
			return ack;
		}
	}

	@Override
	public void close() {
		pipe.close();
	}
	
	@Override
	public String toString() {
		return "ta:" + pipe;
	}
	
	private static class ReceiverAdapter implements BlobReceiver {
		
		private BlobReceiver delegate;
		private AdvancedExecutor executor;
		
		public ReceiverAdapter(BlobReceiver delegate, AdvancedExecutor executor) {
			this.delegate = delegate;
			this.executor = executor;
		}

		@Override
		public FutureEx<Void> blobReceived(final byte[] blob) {
			final FutureBox<Void> ack = new FutureBox<Void>();
			executor.submit(new RunnableEx() {
				@Override
				public void run() {
					try {
						delegate.blobReceived(blob).addListener(ack);
					}
					catch(Exception e) {
						ack.setError(e);
					}
				}
				
				@Override
				public void cancelled() {
					ack.setError(new CancellationException());
				}
			});			
			return ack;
		}

		@Override
		public void closed() {
			delegate.closed();			
		}
	}
}
