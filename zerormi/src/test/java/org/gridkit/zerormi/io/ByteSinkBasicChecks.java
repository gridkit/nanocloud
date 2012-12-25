package org.gridkit.zerormi.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import org.junit.Assert;

public abstract class ByteSinkBasicChecks extends Assert {

	protected abstract ByteSinkTestSupport getSinkTestSuppport();

	protected ByteStreamSink sink() {
		return getSinkTestSuppport().getSink();
	}
	
	protected byte[] read() throws IOException {
		return getSinkTestSuppport().read();
	}

	protected String readString() throws IOException {
		return getSinkTestSuppport().readString();
	}
	
	protected String readAll() throws IOException {
		StringBuilder sb = new StringBuilder();
		while(!isEOF()) {
			sb.append(readString());
		}
		return sb.toString();
	}

	protected boolean isEOF() {
		return getSinkTestSuppport().isEOF();
	}

	protected void pushConsumerException(IOException e) throws ClosedStreamException {
		getSinkTestSuppport().pushConsumerException(e);
	}
	
	public void small_message_whole() throws IOException {
		final String msg = "test message";

		AfterRunner<Void> reader = assertInput(msg);
		
		ByteBuffer wb = ByteBuffer.wrap(msg.getBytes());
		sink().push(wb);
		assertEquals(0, wb.remaining());
		sink().endOfStream();
		
		reader.join();
	}

	public void async_sink_termination() throws IOException {
		AfterRunner<Void> writer = infiniteWriter();
		while(true) {
			byte[] bb = read();
			if (bb.length > 0) {
				break;
			}
		}
		sink().brokenStream(new IOException("Reset"));

		try {
			writer.join();
			fail("Exception expected");
			throw new IOException();
		}
		catch(ClosedStreamException e) {
			// ok
		}
	}

	public void small_message_whole_with_direct_buffer() throws IOException {
		final String msg = "test message";
		
		AfterRunner<Void> reader = assertInput(msg);
		
		ByteBuffer wb = ByteBuffer.allocateDirect(msg.getBytes().length);
		wb.put(msg.getBytes());
		wb.flip();
		sink().push(wb);
		assertEquals(0, wb.remaining());
		sink().endOfStream();
		
		reader.join();
	}

	public void small_message_whole_by_x(int size) throws IOException {
		final String msg = "test message";

		AfterRunner<Void> reader = assertInput(msg);
		
		ByteBuffer bmsg = ByteBuffer.wrap(msg.getBytes());
		byte[] bb = new byte[size];
		
		while(bmsg.remaining() > 0) {
			ByteBuffer wb;
			if (bmsg.remaining() < size) {
				wb = bmsg;
			}
			else {
				bmsg.get(bb);
				wb = ByteBuffer.wrap(bb);
			}
			sink().push(wb);
			assertEquals(0, wb.remaining());			
		}
		sink().endOfStream();
		
		
		reader.join();
	}

	public void data_stream(int lenght, int sendBuffer, boolean direct) throws IOException {
		int mod = 203;
		AfterRunner<Void> reader = assertByteStream(mod, lenght);
		ByteBuffer wb = direct ? ByteBuffer.allocateDirect(sendBuffer) : ByteBuffer.allocate(sendBuffer);
		for(int i = 0; i != lenght; ++i) {
			wb.put((byte)(i % mod));
			if (wb.remaining() == 0) {
				wb.flip();
				sink().push(wb);
				wb.clear();
			}
		}
		if (wb.position() > 0) {
			wb.flip();
			sink().push(wb);			
		}
		sink().endOfStream();
		reader.join();
	}
	
	protected AfterRunner<Void> infiniteWriter() {
		AfterRunner<Void> writer = AfterRunner.create(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				while(true) {
					sink().push(ByteBuffer.wrap("О чём говорить, когда не о чем говорить?".getBytes()));
				}
			}
		});
		return writer;		
	}
	
	protected AfterRunner<Void> assertInput(final String msg) {
		AfterRunner<Void> reader = AfterRunner.create(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				String received = readAll();
				assertEquals(msg, received);
				assertTrue(isEOF());
				return null;
			}
		});
		return reader;
	}

	protected AfterRunner<Void> assertByteStream(final int mod, final int count) {
		AfterRunner<Void> reader = AfterRunner.create(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				int n = 0;
				try {
					while(true) {
						byte[] data = read();				
						for(byte b: data) {
							int v = 0xFF & b;
							assertEquals(n % mod, v);
							++n; 
							if (n > count) {
								fail("Too many data");
							}
						}
					}
				}
				catch(EOFException e) {
					assertEquals(count, n);
					return null;
				}
			}
		});
		return reader;
	}
	
}
