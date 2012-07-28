/**
 * Copyright 2008-2009 Grid Dynamics Consulting Services, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.fabric.exec.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class SocketHelper {

	public static Socket acceptSingleConnection(int port) throws IOException {
		ServerSocket server = new ServerSocket();		
		server.bind(new InetSocketAddress(port));
		Socket accepted = server.accept();
		server.close();
		
		return accepted;
	}

	public static Socket accept(int port, long timeout) throws IOException {
		Timer timer = new Timer("socket-timeout");
		final ServerSocket server = new ServerSocket();		
		server.bind(new InetSocketAddress(port));
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					server.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}, timeout);
		Socket accepted = server.accept();
		server.close();
		
		return accepted;
	}

	public static Socket connect(String host, int port) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host, port));
		
		return socket;
	}

	public static Socket connect(String host, int port, long timeout) throws IOException {
		long deadline = System.currentTimeMillis() + timeout;
		while(true) {
			long waittime = deadline - System.currentTimeMillis();
			if (waittime < 0) {
				throw new SocketTimeoutException();
			}
			if (waittime > 500) {
				waittime = 500;
			}
			try {
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), (int)waittime);
				return socket;
			}
			catch(IOException e) {
				if (deadline <= System.currentTimeMillis()) {
					throw e;
				}
			}
		}
	}
	
}
