package org.gridkit.zerormi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.TaskService;
import org.gridkit.zerormi.ByteStream.Sink;
import org.gridkit.zerormi.ByteStream.SyncBytePipe;

public class Streams {

	public static ByteStream.Duplex[] newSyncPipe() {
		SyncBytePipe sideA = new SyncBytePipe();
		SyncBytePipe sideB = new SyncBytePipe();
		sideA.bind(sideB);
		return new ByteStream.Duplex[]{sideA, sideB};
	}
	
	public static ByteStream.Duplex newPair(String name, ByteStream.InputSocket in, ByteStream.Sink out) {
		return new ByteStream.DuplexPair(name, in, out);
	}
	
	public static ByteStream.Sink toSink(OutputStream os, ComponentSuperviser superviser) {
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
	
	/**
	 * Simple and economic InputStream pumper relaying of {@link InputStream#available()} method.
	 */
	public static class LazyInputStreamPumper implements ByteStream.InputSocket, TaskService.Task {
		
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
				// TODO detect valid socket close cases
				receiver.brokenStream(e);
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
