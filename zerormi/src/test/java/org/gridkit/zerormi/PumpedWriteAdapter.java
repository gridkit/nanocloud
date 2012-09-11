package org.gridkit.zerormi;

import org.gridkit.zerormi.ByteStream.Sink;

public class PumpedWriteAdapter implements ByteStream.Duplex {
	
	private RingBuffer buffer;
	private ByteStream.Duplex socket;
	
	public PumpedWriteAdapter(int size, ByteStream.Duplex socket) {
		this.buffer = new RingBuffer(size);
		this.socket = socket;
	}

	public boolean hasPending() {
		return buffer.used() > 0;
	}
	
	public void pump() {
		buffer.pump(socket.getOutput());
	}

	public void pump(int amount) {
		buffer.pump(socket.getOutput(), amount);
	}
	
	@Override
	public Sink getOutput() {
		return buffer;
	}

	@Override
	public void bindInput(Sink sink) {
		socket.bindInput(sink);
	}
}
