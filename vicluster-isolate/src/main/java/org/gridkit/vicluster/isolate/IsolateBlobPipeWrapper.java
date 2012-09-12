package org.gridkit.vicluster.isolate;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.isolate.Isolate.FutureProxy;
import org.gridkit.zerormi.DuplexBlobPipe;

/**
 * This is a bridge allowing isolate's byte pipe to communicate cross
 * classloader boundaries.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class IsolateBlobPipeWrapper implements DuplexBlobPipe {

	private final Isolate.BlobDuplex duplex;

	public IsolateBlobPipeWrapper(Isolate.BlobDuplex duplex) {
		this.duplex = duplex;
	}

	@Override
	public void bind(DuplexBlobPipe.BlobReceiver receiver) {
		duplex.bind(new SinkToIsolate(receiver));
	}

	@Override
	public FutureEx<Void> sendBinary(byte[] bytes) {
		return new IsolateFutures.FutureUnproxy<Void>(duplex.sendBinary(bytes));
	}

	@Override
	public void close() {
		duplex.close();
	}

	private static class SinkToIsolate implements Isolate.BlobSink {
		
		private final DuplexBlobPipe.BlobReceiver sink;

		public SinkToIsolate(DuplexBlobPipe.BlobReceiver sink) {
			this.sink = sink;
		}

		@Override
		public FutureProxy<Void> blobReceived(byte[] blob) {
			return new IsolateFutures.FutureEnproxy<Void>(sink.blobReceived(blob));
		}

		@Override
		public void closed() {
			sink.closed();
		}
	}
}
