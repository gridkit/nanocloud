package org.gridkit.fabric.remoting;

import java.io.InputStream;
import java.io.OutputStream;

public class NamedStreamPair implements DuplexStream {

	private String name;
	private InputStream in;
	private OutputStream out;
	private boolean closed;
	
	public NamedStreamPair(String name, InputStream in, OutputStream out) {
		this.name = name;
		this.in = in;
		this.out = out;
	}

	@Override
	public InputStream getInput() {
		return in;
	}

	@Override
	public OutputStream getOutput() {
		return out;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public synchronized void close() {
		if (!closed) {
			try {
				in.close();
			}
			catch(Exception e) {
				// ignore;
			}
			try {
				out.close();
			}
			catch(Exception e) {
				// ignore;
			}			
			closed = true;
		}		
	}

	@Override
	public String toString() {
		return name;
	}
}
