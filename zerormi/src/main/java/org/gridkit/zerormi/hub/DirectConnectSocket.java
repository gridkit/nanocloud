package org.gridkit.zerormi.hub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class DirectConnectSocket implements SocketFactory {

	private final SocketAddress addr;
	private final int timeout;

	public DirectConnectSocket(String host, int port) {
		this(new InetSocketAddress(host, port));
	}
	
	public DirectConnectSocket(SocketAddress addr) {
		this(addr, 0);
	}
	
	public DirectConnectSocket(SocketAddress addr, int timeout) {
		this.addr = addr;
		this.timeout = timeout;
	}

	@Override
	public Socket connect() throws IOException {
		Socket sock = new Socket();
		if (timeout > 0) {
			sock.connect(addr, timeout);
		}
		else {
			sock.connect(addr);
		}
		return sock;
	}
	
	@Override
	public String toString() {
		return "connect: " + addr;
	}
}
