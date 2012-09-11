package org.gridkit.zerormi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.zerormi.ByteStream.Sink;

public class ActiveByteDuplex implements ByteStream.Duplex, ByteStream.Sink {

	private final static long POLL_DELAY = 10;
	
	private final String name;
	private final InputStream is;
	private final OutputStream os;
	private final AdvancedExecutor scheduler;
	
	private final byte[] buffer;

	private Sink receiver;
	
	public ActiveByteDuplex(String name, InputStream is, OutputStream os, AdvancedExecutor scheduler) {
		this.name = name;
		this.is = is;
		this.os = os;
		this.scheduler = scheduler;
		this.buffer = new byte[16 << 10];
	}

	@Override
	public Sink getOutput() {
		return this;
	}
	
	

	@Override
	public synchronized void bindInput(Sink sink) {
		if (this.receiver == null) {
			this.receiver = sink;
			start();
		}
		else {
			throw new IllegalStateException("Already bound");
		}		
	}

	private void start() {
		scheduler.exec(new PollTask());
	}

	@Override
	public String toString() {
		return name;
	}

	public class PollTask implements Runnable {
	
		@Override
		public synchronized void run() {			
			int n;
			try {
				n = is.available();
				if (n == 0) {
					scheduler.schedule(this, POLL_DELAY, TimeUnit.MILLISECONDS);
				}
				else {
					
				}
			} catch (IOException e) {
				if (IOHelper.isSocketTerminationException(e)) {
					receiver.streamClose(e);
				}
				else {
					throw new Error();
				}
			}	
		}
		
		@Override
		public String toString() {
			return name + "/PollTask";
		}
	}
}
