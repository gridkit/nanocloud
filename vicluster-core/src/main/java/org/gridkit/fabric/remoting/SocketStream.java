package org.gridkit.fabric.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketStream implements DuplexStream {

	private Socket socket;
	
	public SocketStream(Socket socket) {
		this.socket = socket;
	}

	@Override
	public InputStream getInput() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutput() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		return socket.isClosed();
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

	@Override
	public String toString() {
		return socket.toString();
	}
}
