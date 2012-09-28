package org.gridkit.zerormi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.zerormi.Superviser.SuperviserEvent;

public interface ByteStream {

	public interface Sink {
		
		public void handle(ByteBuffer data);
		
		public void brokenStream(Exception error);
		
		public void endOfStream();
		
	}
	
	public interface InputSocket {

		public void bindInput(ByteStream.Sink sink);
		
	}
	
	public interface Duplex extends InputSocket {

		public boolean isConnected();
		
		public ByteStream.Sink getOutput();
		
		public void bindInput(ByteStream.Sink sink);
		
	}
	
	public interface DuplexConsumer {
		
		public void setStream(ByteStream.Duplex duplex);
		
	}
	
	public static class DuplexPair implements Duplex {
		
		private final String name;
		private final InputSocket input;
		private final Sink output;

		public DuplexPair(String name, InputSocket input, Sink output) {
			this.name = name;
			this.input = input;
			this.output = output;
		}

		@Override
		public boolean isConnected() {
			return true;
		}



		@Override
		public Sink getOutput() {
			return output;
		}

		@Override
		public void bindInput(Sink sink) {
			input.bindInput(sink);
		}
		
		public String toString() {
			return name;
		}
	}
	
	public static class OutputStreamSink implements Sink, Component {

		private final Superviser superviser;
		private final OutputStream output;
		private boolean terminated;
		private Exception lastError;
		
		public OutputStreamSink(Superviser superviser, OutputStream output) {
			this.superviser = superviser;
			this.output = output;
		}

		@Override
		public void handle(ByteBuffer data) {
			try {
				if (data.hasArray()) {
					output.write(data.array(), data.arrayOffset() + data.position(), data.remaining());
				}
				else {
					byte[] buf = new byte[data.remaining()];
					data.get(buf);
					output.write(buf);
				}
			}
			catch(IOException e) {
				System.err.println("Error at stream: " + output.toString());
				lastError = e;
				superviser.onFatalError(SuperviserEvent.newStreamError(this, e));
			}
		}

		@Override
		public void brokenStream(Exception error) {
			lastError = error;
			endOfStream();
		}

		@Override
		public void endOfStream() {
			try {
				System.err.println("Closing stream: " + output.toString());
				terminated = true;
				output.close();
			} catch (IOException e) {
				// ignore
			}
			superviser.onTermination(SuperviserEvent.newClosedEvent(this));
		}

		@Override
		public boolean isInitalized() {
			return true;
		}

		@Override
		public boolean isTerminated() {
			return terminated;
		}

		@Override
		public String getStatusLine() {
			return lastError == null ? "" : lastError.toString();
		}

		@Override
		public void shutdown() {
			endOfStream();
		}

		@Override
		public String toString() {
			return output.toString();
		}
	}
	
	public static class SyncBytePipe implements Duplex, Sink {
		
		private SyncBytePipe counterParty;
		private FutureBox<Sink> counterSink = new FutureBox<ByteStream.Sink>();

		public SyncBytePipe() {			
		}
		
		@Override
		public boolean isConnected() {
			return true;
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
		public void brokenStream(Exception error) {
			other().brokenStream(error);			
		}

		@Override
		public void endOfStream() {
			other().endOfStream();			
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
