package org.gridkit.zerormi.hub;

import java.io.IOException;
import java.net.Socket;

public interface SocketFactory {

	public Socket connect() throws IOException;
	
}
