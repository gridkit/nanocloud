package org.gridkit.gatling.remoting.bootstraper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.gridkit.fabric.remoting.hub.RemotingEndPoint;

public class Bootstraper {

	private String id;
	private String host;
	private int port; 
	private RemotingEndPoint endpoint;
	
	public Bootstraper(String id, String host, int port) {
		this.id = id;
		this.host = host;
		this.port = port;
	}

	public void start() {
		endpoint = new RemotingEndPoint(id, new InetSocketAddress(host, port));
		endpoint.run();
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("Args: " + Arrays.asList(args));
		String id = args[0];
		String host = args[1];
		int port = Integer.valueOf(args[2]);
		
//		Socket socket = new Socket();
//		socket.connect(new InetSocketAddress("127.0.0.1", port));
//		socket.getOutputStream().write("Ping".getBytes());
//		socket.close();
//		
//		System.out.println("Socket ping - OK");
		
		new Bootstraper(id, host, port).start();
		System.exit(0);
	}	
}
