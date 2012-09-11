package org.gridkit.zerormi;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class RingBuffer implements ByteStream.Sink {

	private byte[] buffer;
	private int head;
	private int tail;
	
	public RingBuffer(int bufferSize) {
		buffer = new byte[bufferSize];
	}
	
	@Override
	public synchronized void handle(ByteBuffer data) {
		if (data.remaining() > free()) {
			throw new BufferOverflowException();
		}
		else {
			int start = head;
			head += data.remaining();
			head %= buffer.length;
			if (head > start) {
				data.get(buffer, start, data.remaining());
			}
			else {
				data.get(buffer, start, buffer.length - start);
				data.get(buffer, 0, data.remaining());
			}
		}
	}

	private int free() {
		return buffer.length - used();
	}

	int used() {
		int used = head - tail;
		if (used < 0) {
			used += buffer.length;
		}
		return used;
	}

	@Override
	public void streamClose(Exception error) {
		// ignore
	}

	public void pump(ByteStream.Sink sink) {
		pump(sink, used());
	}

	public void pump(ByteStream.Sink sink, int blockSize) {
		if (blockSize > used()) {
			blockSize = used();
		}
		if (blockSize > 0) {
			ByteBuffer data = ByteBuffer.allocate(blockSize);
			int start = tail;
			tail += data.remaining();
			tail %= buffer.length;
			if (tail > start) {
				data.put(buffer, start, data.remaining());
			}
			else {
				data.put(buffer, start, buffer.length - start);
				data.put(buffer, 0, data.remaining());
			}
	
			data.clear();
			sink.handle(data);
		}
	}	
}
