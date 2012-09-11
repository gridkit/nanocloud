package org.gridkit.zerormi;

import java.nio.ByteBuffer;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;

public interface ByteStream {

	public interface Sink {
		
		public void handle(ByteBuffer data);
		
		public void streamClose(Exception error);
		
	}
	
	public interface Duplex {

		public ByteStream.Sink getOutput();
		
		public void bindInput(ByteStream.Sink sink);
		
	}
	
	public static class SyncBytePipe implements Duplex, Sink {
		
		private SyncBytePipe counterParty;
		private FutureBox<Sink> counterSink = new FutureBox<ByteStream.Sink>();

		public SyncBytePipe() {			
		}
		
		public void bind(SyncBytePipe cp) {
			if (counterParty != null || cp.counterParty != null) {
				throw new IllegalStateException("Already bound");
			}
			else {
				counterParty = cp;
				cp.counterParty = this;
			}
		}
		
		private Sink other() {
			try {
				return counterSink.get();
			} catch (Exception e) {
				throw new RuntimeException();
			}			
		}
		
		@Override
		public void handle(final ByteBuffer data) {
			if (!counterSink.isDone()) {
				counterSink.addListener(new Box<Sink>() {
					@Override
					public void setData(Sink sink) {
						sink.handle(data);					
					}
	
					@Override
					public void setError(Throwable e) {
						e.printStackTrace();
					}
				});
			}
			else {
				other().handle(data);
			}
		}

		@Override
		public void streamClose(Exception error) {
			other().streamClose(error);
		}

		@Override
		public Sink getOutput() {
			return this;
		}

		@Override
		public void bindInput(Sink sink) {
			if (counterParty == null) {
				throw new IllegalStateException("Not bound");
				
			}
			counterParty.counterSink.setData(sink);
		}
	}
}
