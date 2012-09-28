package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.Arrays;

@SuppressWarnings("serial")
public class OutputStreamRemoteAdapter extends OutputStream implements Serializable {

	private final transient OutputStream sink;
	private final RemoteOutputInterface proxy;
	
	public OutputStreamRemoteAdapter(OutputStream sink) {
		this.sink = sink;
		this.proxy = new StreamProxy();
	}
	
	@Override
	public void write(int b) throws IOException {
		proxy.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		proxy.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		proxy.write(Arrays.copyOfRange(b, off, off + len));
	}

	@Override
	public void flush() throws IOException {
		proxy.flush();
	}

	@Override
	public void close() throws IOException {
		proxy.close();
	}

	private static interface RemoteOutputInterface extends Remote {
		
		public void write(int b) throws IOException;
		
		public void write(byte[] b) throws IOException;

		public void flush() throws IOException;

		public void close() throws IOException;
	}
	
	private class StreamProxy implements RemoteOutputInterface {

		@Override
		public void write(int b) throws IOException {
			sink.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			sink.write(b);
		}

		@Override
		public void flush() throws IOException {
			sink.flush();
		}

		@Override
		public void close() throws IOException {
			sink.close();
		}
	}
}
