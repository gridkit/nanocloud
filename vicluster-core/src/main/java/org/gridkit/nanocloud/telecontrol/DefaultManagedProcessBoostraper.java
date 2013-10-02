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
package org.gridkit.nanocloud.telecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.nanocloud.telecontrol.HostControlConsole.Destroyable;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.SocketHandler;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.NamedStreamPair;
import org.gridkit.zerormi.SocketStream;
import org.gridkit.zerormi.hub.MasterHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.gridkit.zerormi.hub.SlaveSpore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class DefaultManagedProcessBoostraper {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManagedProcessBoostraper.class);
	

	public ManagedProcess createProcess(Map<String, Object> configuration) {
		HostControlConsole console = (HostControlConsole) configuration.get("#boostrap:control-console");
		MasterHub masterHub = (MasterHub) configuration.get("#boostrap:master-hub");
		String name = new ViConf(configuration).getNodeName();
		
		ControlledSession session = new ControlledSession();
		session.sessionId = name;
		session.hub = masterHub;
		
		SlaveSpore spore = masterHub.allocateSession(name, session);

		// TODO
		return null;
	}
	
	private static class CallbackSporePlanter implements Runnable, Serializable {

		private static final long serialVersionUID = 20130928L;
		
		SlaveSpore spore;
		String masterHost;
		int masterPort;
		
		@Override
		public void run() {
			spore.start(new ConnectSocketConnector(new InetSocketAddress(masterHost, masterPort)));
		}

		@Override
		public String toString() {
			return spore + " + call home [" + masterHost + ":" + masterPort + "]";
		}
	}
	
	private static class ConnectSocketConnector implements DuplexStreamConnector {

		private final SocketAddress address;
		
		public ConnectSocketConnector(SocketAddress address) {
			this.address = address;
		}

		@Override
		public DuplexStream connect() throws IOException {
			Socket socket = new Socket();
			socket.connect(address);

			return new SocketStream(socket);
		}
		
		@Override
		public String toString() {
			return String.valueOf(address);
		}
	}
	
	private static class ProcessStreams {
		
		OutputStream stdIn;
		InputStream stdOut;
		InputStream stdErr;
		
	}
	
	private static class ControlledSession implements SessionEventListener, ManagedProcess, ProcessHandler, SocketHandler {

		MasterHub hub;
		String sessionId;
		FutureBox<SocketAddress> bindAddress = new FutureBox<SocketAddress>();
		FutureBox<ProcessStreams> procStreams = new FutureBox<ProcessStreams>();
		FutureBox<Integer> exitCode = new FutureBox<Integer>();
		FutureBox<AdvancedExecutor> executor = new FutureBox<AdvancedExecutor>();
		Destroyable socketHandle;
		volatile Destroyable procHandle;
		
		@Override
		public void bound(String host, int port) {
			bindAddress.setData(new InetSocketAddress(host, port));			
		}

		@Override
		public void accepted(String remoteHost, int remotePort, InputStream soIn, OutputStream soOut) {
			// TODO logging
			hub.dispatch(new NamedStreamPair("tunnled(" + remoteHost + ":" + remotePort + ")", soIn, soOut));
		}

		@Override
		public void terminated(String message) {
			if (!executor.isDone()) {
				executor.setError(new IOException("Transport terminated: " + message));
			}
		}

		@Override
		public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
			ProcessStreams ps = new ProcessStreams();
			ps.stdIn = stdIn;
			ps.stdOut = stdOut;
			ps.stdErr = stdErr;
			procStreams.setData(ps);
		}

		@Override
		public void finished(int exitCode) {
			this.exitCode.setData(exitCode);
			hub.dropSession(sessionId);
		}

		@Override
		public AdvancedExecutor getExecutionService() {
			return fget(executor);
		}

		@Override
		public void bindStdIn(InputStream is) {
			ProcessStreams ps = fget(procStreams);
			if (is != null) {
				BackgroundStreamDumper.link(is, ps.stdIn);
			}
			else {
				try {
					ps.stdIn.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public void bindStdOut(OutputStream os) {
			ProcessStreams ps = fget(procStreams);
			if (os != null) {
				BackgroundStreamDumper.link(ps.stdOut, os);
			}
			else {
				try {
					ps.stdOut.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public void bindStdErr(OutputStream os) {
			ProcessStreams ps = fget(procStreams);
			if (os != null) {
				BackgroundStreamDumper.link(ps.stdErr, os);
			}
			else {
				try {
					ps.stdErr.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		@Override
		public void suspend() {
			throw new UnsupportedOperationException();
			
		}

		@Override
		public void resume() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void destroy() {
			hub.dropSession(sessionId);
			closed();
		}

		@Override
		public FutureEx<Integer> getExitCodeFuture() {
			return exitCode;
		}
		
		private <T> T fget(Future<T> future) {
			try {
				return future.get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RuntimeException) {
					throw (RuntimeException)e.getCause();
				}
				else if (e.getCause() instanceof Error) {
					throw (Error)e.getCause();
				}
				else {
					throw new RuntimeException(e.getCause());
				}
			}
		}

		@Override
		public void connected(DuplexStream stream) {
			executor.setData(hub.getExecutionService(sessionId));
		}

		@Override
		public void interrupted(DuplexStream stream) {
			// ignore
		}

		@Override
		public void reconnected(DuplexStream stream) {
			// ignore
		}

		@Override
		public void closed() {
			if (procHandle != null) {
				procHandle.destroy();
			}
		}
	}	
}
