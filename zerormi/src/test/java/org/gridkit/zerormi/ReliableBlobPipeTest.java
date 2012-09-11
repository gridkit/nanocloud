package org.gridkit.zerormi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.ByteStream.Duplex;
import org.junit.Before;
import org.junit.Test;

public class ReliableBlobPipeTest {
	
	private static final int TEST_TIMEOUT = Integer.MAX_VALUE;
//	private static final int TEST_TIMEOUT = 1000;
	
	private ByteStream.SyncBytePipe syncSocketA;
	private ByteStream.SyncBytePipe syncSocketB;
	
	private PumpedWriteAdapter pumpA;
	private PumpedWriteAdapter pumpB;
	
	private ReliableBlobPipe sideA;
	private ReliableBlobPipe sideB;
	
	private BlobBuffer bufferA;
	private BlobBuffer bufferB;
	
	@Before
	public void initSocket() {
		syncSocketA = new ByteStream.SyncBytePipe();
		syncSocketB = new ByteStream.SyncBytePipe();
		
		syncSocketA.bind(syncSocketB);

		sideA = new ReliableBlobPipe("sideA", new Superviser());
		sideB = new ReliableBlobPipe("sideB", new Superviser());
	
		bufferA = new BlobBuffer();
		bufferB = new BlobBuffer();
		
		sideA.bind(bufferA);
		sideB.bind(bufferB);
		
		sideA.setStream(syncSocketA);
		sideB.setStream(syncSocketB);
	}
	
	public void initWriteBuffer(int capacity) {
		syncSocketA = new ByteStream.SyncBytePipe();
		syncSocketB = new ByteStream.SyncBytePipe();
		
		syncSocketA.bind(syncSocketB);

		pumpA = new PumpedWriteAdapter(capacity, syncSocketA);
		pumpB = new PumpedWriteAdapter(capacity, syncSocketB);
		
		sideA.setStream(pumpA);
		sideB.setStream(pumpB);
	}
	
	@Test(timeout = TEST_TIMEOUT)
	public void verify_passthrough() throws InterruptedException, ExecutionException {
		byte[] hello_world = "hello_world".getBytes();
		Future<Void> ack = sideA.sendBinary(hello_world);
		
		assert_buffer_content(bufferB, hello_world);
		ack.get();

		assert_that_pending_queue_is_empty(sideA);
		assert_that_pending_queue_is_empty(sideB);
	}

	@Test(timeout = TEST_TIMEOUT)
	public void verify_passthrough_2() throws InterruptedException, ExecutionException {
		byte[] hello_world = "hello_world".getBytes();
		byte[] hello_world2 = "hi".getBytes();
		Future<Void> ack1 = sideA.sendBinary(hello_world);
		Future<Void> ack2 = sideA.sendBinary(hello_world2);
		
		assert_buffer_content(bufferB, hello_world, hello_world2);
		ack1.get();
		ack2.get();

		assert_that_pending_queue_is_empty(sideA);
		assert_that_pending_queue_is_empty(sideB);
	}

	@Test(timeout = TEST_TIMEOUT)
	public void verify_nack() throws InterruptedException, ExecutionException {
		byte[] hello_world = "hello_world".getBytes();
		planErrorAtB();
		
		Future<Void> ack1 = sideA.sendBinary(hello_world);
		Future<Void> ack2 = sideA.sendBinary(hello_world);
		
		assert_buffer_content(bufferB, hello_world, hello_world);
		assert_pending_exception(ack1);
		ack2.get();
		assert_that_pending_queue_is_empty(sideA);
		assert_that_pending_queue_is_empty(sideB);
	}

	@Test(timeout = TEST_TIMEOUT)
	public void verify_buffered_passthrough_2() throws InterruptedException, ExecutionException {
		initWriteBuffer(4 << 10);
		byte[] hello_world = "hello_world".getBytes();
		byte[] hello_world2 = "hi".getBytes();
		Future<Void> ack1 = sideA.sendBinary(hello_world);
		Future<Void> ack2 = sideA.sendBinary(hello_world2);
		
		pumpA.pump();
		pumpB.pump();
		
		assert_buffer_content(bufferB, hello_world, hello_world2);
		ack1.get();
		ack2.get();
		
		Assert.assertEquals(0, sideA.getOutboundQueueSize());
		Assert.assertEquals(2, sideB.getInboundQueueSize());

		pumpA.pump();

		Assert.assertEquals(0, sideB.getInboundQueueSize());
	}

	@Test(timeout = TEST_TIMEOUT)
	public void verify_buffered_nack() throws InterruptedException, ExecutionException {
		initWriteBuffer(4 << 10);
		byte[] hello_world = "hello_world".getBytes();
		byte[] hello_world2 = "hi".getBytes();
		Future<Void> ack1 = sideA.sendBinary(hello_world);
		Future<Void> ack2 = sideA.sendBinary(hello_world2);
		
		planErrorAtB();
		
		pumpA.pump();
		pumpB.pump();
		
		assert_buffer_content(bufferB, hello_world, hello_world2);
		assert_pending_exception(ack1);
		ack2.get();
		
		Assert.assertEquals(0, sideA.getOutboundQueueSize());
		Assert.assertEquals(2, sideB.getInboundQueueSize());
	
		pumpA.pump();
	
		assert_that_pending_queue_is_empty(sideA);
		assert_that_pending_queue_is_empty(sideB);
	}

	@Test(timeout = TEST_TIMEOUT)
	public void verify_resync1_nack() throws InterruptedException, ExecutionException {
		initWriteBuffer(4 << 10);
		
		byte[] hello_world = "hello_world".getBytes();
		byte[] hello_world2 = "hi".getBytes();
		Future<Void> ack1 = sideA.sendBinary(hello_world);
		Future<Void> ack2 = sideA.sendBinary(hello_world2);
		
		planErrorAtB();
		
		// this will simulate reconnection
		initWriteBuffer(4 << 10);
		
		pumpA.pump();
		pumpB.pump();
		
		// pipes require extra round trip to resync
		pumpA.pump();
		pumpB.pump();
		
		assert_buffer_content(bufferB, hello_world, hello_world2);
		assert_pending_exception(ack1);
		ack2.get();
		
		Assert.assertEquals(0, sideA.getOutboundQueueSize());
		Assert.assertEquals(2, sideB.getInboundQueueSize());
	
		pumpA.pump();
	
		assert_that_pending_queue_is_empty(sideA);
		assert_that_pending_queue_is_empty(sideB);
	}

	@Test(timeout = TEST_TIMEOUT)
	public void verify_resync2_nack() throws InterruptedException, ExecutionException {
		initWriteBuffer(4 << 10);
		
		byte[] hello_world = "hello_world".getBytes();
		byte[] hello_world2 = "hihi".getBytes();
		Future<Void> ack1 = sideA.sendBinary(hello_world);
		Future<Void> ack2 = sideA.sendBinary(hello_world2);
		
		planErrorAtB();
		
		pumpA.pump();

		assert_buffer_content(bufferB, hello_world, hello_world2);

		Assert.assertEquals(2, sideA.getOutboundQueueSize());
		Assert.assertEquals(2, sideB.getInboundQueueSize());

		System.out.println("Replacing socket");
		// this will simulate reconnection
		initWriteBuffer(4 << 10);
		
		pumpB.pump();

		ack2.get();

		Assert.assertEquals(1, sideA.getOutboundQueueSize());

		pumpA.pump();
		
		Assert.assertEquals(1, sideB.getInboundQueueSize());

		pumpB.pump();

		assert_pending_exception(ack1);

		Assert.assertEquals(0, sideA.getOutboundQueueSize());
			
		pumpA.pump();
	
		assert_that_pending_queue_is_empty(sideA);
		assert_that_pending_queue_is_empty(sideB);
	}
	
	private void planErrorAtB() {
		bufferB.nextException = new RuntimeException("Test");
	}

	private void assert_pending_exception(Future<Void> ack)
			throws InterruptedException {
		try {
			ack.get();
		}
		catch(ExecutionException e) {
			Assert.assertEquals(
					"java.io.IOException: Remote error: java.lang.RuntimeException: Test", 
					e.getCause().toString());
		}
	}

	private void assert_buffer_content(BlobBuffer buffer, byte[]... messages) {
		Assert.assertEquals("Buffer content", toString(buffer), toString(messages));
	}
	
	private void assert_that_pending_queue_is_empty(ReliableBlobPipe pipe) {
		Assert.assertEquals("Inbound queue should be empty", 0, pipe.getInboundQueueSize());
		Assert.assertEquals("Outbound queue should be empty", 0, pipe.getOutboundQueueSize());
	}

	private String toString(BlobBuffer buffer) {
		StringBuilder sb = new StringBuilder();
		for(byte[] data : buffer.buffer) {
			if (sb.length() > 0) {
				sb.append("|");
			}
			sb.append(new String(data));
		}
		return sb.toString();
	}

	private String toString(byte[]... buffer) {
		StringBuilder sb = new StringBuilder();
		for(byte[] data : buffer) {
			if (sb.length() > 0) {
				sb.append("|");
			}
			sb.append(new String(data));
		}
		return sb.toString();
	}

	private static class BlobBuffer implements DuplexBlobPipe.BlobReceiver {
		
		List<byte[]> buffer = new ArrayList<byte[]>();
		Exception nextException;
		@SuppressWarnings("unused")
		boolean closed;

		@Override
		public FutureEx<Void> blobReceived(byte[] blob) {
			buffer.add(blob);
			if (nextException != null) {
				FutureEx<Void> ack = FutureBox.errorFuture(nextException);
				nextException = null;
				return ack;
			}
			else {
				return FutureBox.dataFuture(null);
			}
		}

		@Override
		public void closed() {
			closed = true;			
		}
	}
	
	private static class Superviser implements ReliableBlobPipe.PipeSuperviser {

		@Override
		public void onWarning(SuperviserEvent event) {
			System.err.println(event);
		}

		@Override
		public void onTermination(SuperviserEvent event) {
			System.err.println(event);
		}

		@Override
		public void onFatalError(SuperviserEvent event) {
			System.err.println(event);
		}

		@Override
		public void onStreamRejected(ReliableBlobPipe pipe, Duplex stream,	Exception e) {
			System.out.println("Stream rejected: " + e);
			e.printStackTrace();
		}
	}
}
