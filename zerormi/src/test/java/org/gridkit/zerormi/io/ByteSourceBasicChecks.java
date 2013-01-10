package org.gridkit.zerormi.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.TimedFuture;
import org.junit.Assert;

public abstract class ByteSourceBasicChecks extends Assert {

	protected abstract ByteSourceTestSupport getSourceTestSuppport();
	
	protected ByteStreamSource source() {
		return getSourceTestSuppport().getSource();
	}
	
	protected void write(byte[] data) throws IOException {
		getSourceTestSuppport().write(data);
	}

	protected void write(String text) throws IOException {
		getSourceTestSuppport().write(text);
	}
	
	protected void eof() throws ClosedStreamException {
		getSourceTestSuppport().closeOutput();
	}

	protected void pushProducerException(IOException e) throws ClosedStreamException {
		getSourceTestSuppport().pushProducerException(e);
	}

	public void small_message_whole() throws IOException {
		final String msg = "test message";

		AfterRunner<Void> writer = solidWriter(msg);
		
		ByteBuffer wb = ByteBuffer.wrap(msg.getBytes());
		readUntilEof(wb);
		
		assertEquals(0, wb.remaining());
		
		writer.join();
	}

	public void small_message_whole_with_direct_buffer() throws IOException {
		final String msg = "test message";
		
		AfterRunner<Void> writer = solidWriter(msg);
		
		ByteBuffer wb = ByteBuffer.allocateDirect(msg.getBytes().length);
		readUntilEof(wb);
		
		assertEquals(0, wb.remaining());
		
		writer.join();
	}

	protected void write_whole_read_by_x(String text, int readBufferSize, boolean direct) throws IOException {
		AfterRunner<Void> writer = solidWriter(text);

		ByteBuffer rb = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
		
		Assert.assertEquals(text, readTextUntilEof(rb));
		
		writer.join();
	}
	
	protected void async_source_termination(String text, int readBufferSize, boolean direct, boolean delayReader, boolean delayClose) throws IOException, InterruptedException {
		ByteBuffer buf = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
		AfterRunner<String> reader = delayReader ? delayedWholeReader(50, buf) : wholeReader(buf);
		if (text.length() > 0) {
			write(text);
		}
		if (delayClose) {
			Thread.sleep(50);
		}
		source().brokenStream(new IOException("Reset"));

		try {
			reader.join();
			fail("Exception expected");
			throw new IOException();
		}
		catch(ClosedStreamException e) {
			// ok
		}
	}

	public void available_state_after_eof() throws IOException {
		eof();
		assertTrue(source().available() == -1);
		try {
			source().pull(ByteBuffer.allocate(64));
			fail("ClosedStreamException exception expected");
		}
		catch(ClosedStreamException e) {
			// expected
		}
	}

	public void available_state_after_reader_close() throws IOException {
		source().brokenStream(new IOException("Reset"));
		assertTrue(source().available() > 0);
		try {
			source().pull(ByteBuffer.allocate(64));
			fail("ClosedStreamException exception expected");
		}
		catch(ClosedStreamException e) {
			// expected
		}
		// ClosedStreamException should be thrown only once
		assertTrue(source().available() == -1);
	}

	protected void data_stream_until_eof(int length, int sendBuffer, boolean direct) throws IOException {
		int mod = 203;
		AfterRunner<Void> writer = produceByteStream(mod, length);
		ByteBuffer wb = direct ? ByteBuffer.allocateDirect(sendBuffer) : ByteBuffer.allocate(sendBuffer);
		int n = 0;
		try {
			while(true) {
				wb.clear();
				source().pull(wb);
				wb.flip();
				byte[] bb = new byte[wb.remaining()];
				wb.get(bb);
				for(byte b: bb) {
					int v = 0xFF & b;
					assertEquals(n % mod, v);
					++n; 
					if (n > length) {
						fail("Too many data");
					}
				}
			}
		}
		catch(EOFException e) {
			assertEquals(length, n);
		}
		writer.join();
	}

	protected void data_stream_while_available(int length, int sendBuffer, boolean direct) throws IOException {
		int mod = 203;
		AfterRunner<Void> writer = produceByteStream(mod, length);
		ByteBuffer wb = direct ? ByteBuffer.allocateDirect(sendBuffer) : ByteBuffer.allocate(sendBuffer);
		int n = 0;
		try {
			while(source().available() >= 0) {
				wb.clear();
				source().pull(wb);
				wb.flip();
				byte[] bb = new byte[wb.remaining()];
				wb.get(bb);
				for(byte b: bb) {
					int v = 0xFF & b;
					assertEquals(n % mod, v);
					++n; 
					if (n > length) {
						fail("Too many data");
					}
				}
			}
		}
		catch(EOFException e) {
			assertEquals(length, n);
		}
		writer.join();
	}

	
	private void readUntilEof(ByteBuffer wb) throws IOException {
		while(source().available() >= 0) {
			source().pull(wb);
		}
	}

	private String readTextUntilEof(ByteBuffer wb) throws IOException {
		StringBuilder sb = new StringBuilder();
		while(source().available() >= 0) {
			wb.clear();
			source().pull(wb);
			wb.flip();
			if (wb.remaining() > 0) {
				byte[] bb = new byte[wb.remaining()];
				wb.get(bb);
				sb.append(new String(bb));
			}
		}
		return sb.toString();
	}

	private AfterRunner<Void> solidWriter(final String msg) {
		return new AfterRunner<Void>(FutureBox.dataFuture(null), new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				write(msg);
				eof();
				return null;
			}
		});
	}	

	private AfterRunner<String> wholeReader(final ByteBuffer buffer) {
		return new AfterRunner<String>(FutureBox.dataFuture(null), new Callable<String>() {
			@Override
			public String call() throws Exception {
				return readTextUntilEof(buffer);
			}
		});
	}	

	private AfterRunner<String> delayedWholeReader(int delay, final ByteBuffer buffer) {
		return new AfterRunner<String>(TimedFuture.delay(delay, TimeUnit.MILLISECONDS), new Callable<String>() {
			@Override
			public String call() throws Exception {
				return readTextUntilEof(buffer);
			}
		});
	}	
	
	protected AfterRunner<Void> produceByteStream(final int mod, final int count) {
		AfterRunner<Void> reader = AfterRunner.create(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				int n = 0;
				while(n < count) {
					byte b = (byte)(0xFF & (n % mod));
					write(new byte[]{b});
					++n;
				}
				eof();
				return null;
			}
		});
		return reader;
	}	
}
