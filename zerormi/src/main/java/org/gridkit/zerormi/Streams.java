package org.gridkit.zerormi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.TaskService;
import org.gridkit.zerormi.ByteStream.DuplexPair;
import org.gridkit.zerormi.ByteStream.Sink;
import org.gridkit.zerormi.ByteStream.SyncBytePipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Streams {
	
	static final Logger LOGGER = LoggerFactory.getLogger(Streams.class);

	public static ByteStream.Duplex[] newSyncPipe() {
		SyncBytePipe sideA = new SyncBytePipe();
		SyncBytePipe sideB = new SyncBytePipe();
		sideA.bind(sideB);
		return new ByteStream.Duplex[]{sideA, sideB};
	}
	
	public static ByteStream.Duplex newPair(String name, ByteStream.InputSocket in, ByteStream.Sink out) {
		return new ByteStream.DuplexPair(name, in, out);
	}
	
	public static ByteStream.Sink toSink(OutputStream os, Superviser superviser) {
		return new ByteStream.OutputStreamSink(superviser, os);
	}

	public static ByteStream.InputSocket toSocket(InputStream is, TaskService executor, boolean busy) {
		if (busy) {
			throw new UnsupportedOperationException();
		}
		else {
			return new LazyInputStreamPumper(is, executor);
		}
	}
	
	public static ByteStream.Duplex toDuplex(final Socket socket, TaskService taskService) throws IOException {

		Superviser superviser = new Superviser.GenericSuperviser() {
			
			@Override
			protected void report(SuperviserEvent event) {
				if (event.isWarning()) {
					LOGGER.warn(event.toString());
				}
				else if (event.isError()) {
					LOGGER.error(event.toString());
				}
				else {
					LOGGER.info(event.toString());
				}
			}
			
			@Override
			protected void onTerminate(Object object) {
				try {
					socket.close();
				} catch(IOException e) {
					// ignore
				}				
			}
			
			@Override
			protected void onError(Object object) {
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
				}				
			}
		};
		
		ByteStream.InputSocket sock = toSocket(socket.getInputStream(), taskService, false);
		ByteStream.Sink sink = toSink(socket.getOutputStream(), superviser);
		
		return new DuplexPair(socket.toString(), sock, sink) {
			@Override
			public boolean isConnected() {
				return socket.isConnected() && !socket.isClosed();
			}
		};
	}

	public static ByteStream.Duplex toDuplex(final DuplexStream stream, TaskService taskService) throws IOException {
		
		Superviser superviser = new Superviser.GenericSuperviser() {
			
			@Override
			protected void report(SuperviserEvent event) {
				if (event.isWarning()) {
					LOGGER.warn(event.toString());
				}
				else if (event.isError()) {
					LOGGER.error(event.toString());
				}
				else {
					LOGGER.info(event.toString());
				}
			}
			
			@Override
			protected void onTerminate(Object object) {
				try {
					stream.close();
				} catch(IOException e) {
					// ignore
				}				
			}
			
			@Override
			protected void onError(Object object) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}				
			}
		};
		
		ByteStream.InputSocket sock = toSocket(stream.getInput(), taskService, false);
		ByteStream.Sink sink = toSink(stream.getOutput(), superviser);
		
		return new DuplexPair(stream.toString(), sock, sink) {
			@Override
			public boolean isConnected() {
				return !stream.isClosed();
			}
		};
	}
	
	/**
	 * Simple and economic InputStream pumper relaying of {@link InputStream#available()} method.
	 */
	static class LazyInputStreamPumper implements ByteStream.InputSocket, TaskService.Task {
		
		private static final int BUFFER_LIMIT = 16 << 10;
		private static final long POLL_DELAY = TimeUnit.MILLISECONDS.toNanos(10);
		
		private final InputStream is;
		private final TaskService taskService;
		
		private ByteStream.Sink receiver;

		public LazyInputStreamPumper(InputStream is, TaskService taskService) {
			this.is = is;
			this.taskService = taskService;
		}

		@Override
		public synchronized void bindInput(Sink sink) {
			if (receiver == null) {
				receiver = sink;
				reschedule();
			}
			else {
				throw new IllegalStateException("Already bound");
			}
		}

		@Override
		public void run() {
			try {
				int avail = is.available();
				if (avail > BUFFER_LIMIT) {
					avail = BUFFER_LIMIT;
				}
				if (avail == 0) {
					rescheduleWithDelay();
				}
				else {
					byte[] buf = new byte[avail];
					int amount = is.read(buf);
					receiver.handle(ByteBuffer.wrap(buf, 0, amount));
					reschedule();
				}
			}
			catch(Exception e) {
				if (IOHelper.isSocketTerminationException(e)) {
					receiver.endOfStream();
				}
				else {
					receiver.brokenStream(e);
				}
			}			
		}

		private void reschedule() {
			try {
				taskService.schedule(this);
			}
			catch(IllegalStateException e) {
				receiver.brokenStream(new CancellationException());
			}
		}

		private void rescheduleWithDelay() {
			try {
				taskService.schedule(this, POLL_DELAY, TimeUnit.NANOSECONDS);
			}
			catch(IllegalStateException e) {
				receiver.brokenStream(new CancellationException());
			}
		}

		@Override
		public void interrupt(Thread taskThread) {
			// TODO it is possible to miss end of stream this way
			try {
				is.close();
			}
			catch(IOException e) {
				// ignore
			}
		}

		@Override
		public void cancled() {
			try {
				is.close();
			} catch (IOException e) {
				// ignore
			}
			receiver.brokenStream(new CancellationException());
		}
	}
}
