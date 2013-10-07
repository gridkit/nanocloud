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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
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
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.bootstraper.SmartBootstraper;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.NamedStreamPair;
import org.gridkit.zerormi.SocketStream;
import org.gridkit.zerormi.hub.MasterHub;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;
import org.gridkit.zerormi.hub.SlaveSpore;
import org.gridkit.zerormi.zlog.ZLogFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ProcessSporeLauncher implements ProcessLauncher {

	@Override
	public ManagedProcess createProcess(Map<String, Object> configuration) {
		HostControlConsole console = (HostControlConsole) configuration.get("#boostrap:control-console");
		MasterHub masterHub = (MasterHub) configuration.get("#boostrap:master-hub");
		String name = new ViConf(configuration).getNodeName();
		
		ControlledSession session = new ControlledSession();
		session.sessionId = name;
		session.hub = masterHub;
		
		SlaveSpore spore = masterHub.allocateSession(name, session);
		session.spore = spore;

		Destroyable socketHandler = console.openSocket(session);
		session.socketHandle = socketHandler;
		
		InetSocketAddress sockAddr = (InetSocketAddress)fget(session.bindAddress);
		CallbackSporePlanter planter = new CallbackSporePlanter(spore, sockAddr.getHostName(), sockAddr.getPort());
		byte[] binspore = serialize(planter);
		session.binspore = binspore;
		
		String classpath = System.getProperty("java.class.path");
		JvmConfig jc = new JvmConfig();
		classpath = jc.filterClasspath(classpath);
		
		String javaCmd = new File(new File(new File(System.getProperty("java.home")), "bin"), "java").getPath();
		
		List<String> commands = new ArrayList<String>();
		commands.add(javaCmd);
		commands.add("-cp");
		commands.add("\"" + classpath +"\"");
		commands.add("-D" + ZLogFactory.PROP_ZLOG_MODE + "=slf4j");
		commands.add(SmartBootstraper.class.getName());
		
		console.startProcess(".", commands.toArray(new String[0]), null, session);
		
		return session;
	}

	private byte[] serialize(Object obj) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.flush();
			oos.close();
			byte[] binspore = bos.toByteArray();
			return binspore;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	static <T> T fget(Future<T> future) {
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
	
	private static class CallbackSporePlanter implements Runnable, Serializable {

		private static final long serialVersionUID = 20130928L;
		
		SlaveSpore spore;
		String masterHost;
		int masterPort;
		
		public CallbackSporePlanter(SlaveSpore spore, String masterHost, int masterPort) {
			this.spore = spore;
			this.masterHost = masterHost;
			this.masterPort = masterPort;
		}

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
		SlaveSpore spore;
		byte[] binspore;
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
			hub.dispatch(new NamedStreamPair("tunnel(" + remoteHost + ":" + remotePort + ")", soIn, soOut));
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
			try {
				DataOutputStream dos = new DataOutputStream(stdIn);
				dos.writeInt(binspore.length);
				dos.write(binspore);
				dos.flush();
			} catch (IOException e) {
				procStreams.setError(e);
				executor.setError(e);
				procHandle.destroy();
				return;
			}
			procStreams.setData(ps);
		}

		@Override
		public void finished(int exitCode) {
			this.exitCode.setData(exitCode);
			hub.terminateSpore(spore);
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
			hub.terminateSpore(spore);
			closed();
		}

		@Override
		public FutureEx<Integer> getExitCodeFuture() {
			return exitCode;
		}
		
		@Override
		public void connected(DuplexStream stream) {
			executor.setData(hub.getSlaveExecutor(spore));
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
