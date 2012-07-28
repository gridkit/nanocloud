package org.gridkit.fabric.remoting.hub;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.gridkit.fabric.remoting.DuplexStream;
import org.gridkit.fabric.remoting.SocketStream;

/**
 * This is a simple socket server passing incoming connections to {@link RemotingHub} 
 * which authenticates them and processing RMI handshake. 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SimpleSocketAcceptor implements Runnable {

	private RemotingHub hub;
	private ServerSocket socket;
	private Thread acceptor;
	
	public SimpleSocketAcceptor() {		
	}
	
	public void bind(ServerSocket socket, RemotingHub hub) {
		this.socket = socket;
		this.hub = hub;
	}
	
	public void start() {
		acceptor = new Thread(this);
		acceptor.start();
	}

	@Override
	public void run() {
		try {
			while(true) {
				Socket con =socket.accept();
				System.out.println("Connection accepted: " + con.getRemoteSocketAddress());
				DuplexStream ds = new SocketStream(con);
				hub.dispatch(ds);
			}
		} catch (IOException e) {
			e.printStackTrace();			
		}
	}
	
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// ignore
		}
	}
}
