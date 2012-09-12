package org.gridkit.zerormi;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;


public interface DuplexBlobPipe {

	public void bind(BlobReceiver receiver);
	
	public FutureEx<Void> sendBinary(byte[] bytes);
	
	public void close();	
	
	public interface BlobReceiver {
	
		public FutureEx<Void> blobReceived(byte[] blob);
		
		public void closed();		
	}	
	
	public static class SyncBlobPipe implements DuplexBlobPipe {
		
		private SyncBlobPipe counterParty;
		private FutureBox<BlobReceiver> counterSink = new FutureBox<BlobReceiver>();

		public SyncBlobPipe() {			
		}
		
		public void bind(SyncBlobPipe cp) {
			if (counterParty != null || cp.counterParty != null) {
				throw new IllegalStateException("Already bound");
			}
			else {
				counterParty = cp;
				cp.counterParty = this;
			}
		}
		
		@Override
		public void bind(BlobReceiver receiver) {
			if (counterParty == null) {
				throw new IllegalStateException("Not bound");
				
			}
			counterParty.counterSink.setData(receiver);
		}
		
		private BlobReceiver other() {
			try {
				return counterSink.get();
			} catch (Exception e) {
				throw new RuntimeException();
			}			
		}
		
		@Override
		public FutureEx<Void> sendBinary(final byte[] data) {
			if (!counterSink.isDone()) {
				final FutureBox<Void> ack = new FutureBox<Void>();
				counterSink.addListener(new Box<BlobReceiver>() {
					@Override
					public void setData(BlobReceiver receiver) {
						receiver.blobReceived(data).addListener(ack);					
					}
	
					@Override
					public void setError(Throwable e) {
						ack.setError(e);
					}
				});
				return ack;
			}
			else {
				return other().blobReceived(data);
			}
		}
				
		@Override
		public void close() {
			other().closed();			
		}
	}
}
