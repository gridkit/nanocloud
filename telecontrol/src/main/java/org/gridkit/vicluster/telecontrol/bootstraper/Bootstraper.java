/**
 * Copyright 2012 Alexey Ragozin
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
package org.gridkit.vicluster.telecontrol.bootstraper;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.TimerTask;

import org.gridkit.zerormi.hub.RemotingEndPoint;

/**
 * This is a main class to be started on remotly controlled JVM.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
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
		endpoint.enableHeartbeatDeatchWatch();
		endpoint.run();
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("Exec command: " + ManagementFactory.getRuntimeMXBean().getInputArguments().toString());
		String id = args[0];
		String host = args[1];
		int port = Integer.valueOf(args[2]);
		
		new Bootstraper(id, host, port).start();
		System.exit(0);
	}	
}
