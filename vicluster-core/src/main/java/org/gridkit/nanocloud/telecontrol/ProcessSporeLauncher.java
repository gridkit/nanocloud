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
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.gridkit.nanocloud.telecontrol.HostControlConsole.Destroyable;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.SocketHandler;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.SpiContext;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.ClasspathUtils;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.bootstraper.SmartBootstraper;
import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.NamedStreamPair;
import org.gridkit.zerormi.SocketStream;
import org.gridkit.zerormi.hub.SlaveSpore;
import org.gridkit.zerormi.zlog.ZLogFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ProcessSporeLauncher implements ProcessLauncher {

	@Override
	public ManagedProcess createProcess(Map<String, Object> config) {
		SpiContext ctx = ViEngine.asSpiConfig(config);
		HostControlConsole console = ctx.getControlConsole();
		RemoteExecutionSession rmiSession = ctx.getRemotingSession();
		
		ControlledSession session = new ControlledSession();
		session.session = rmiSession;
		
		SlaveSpore spore = rmiSession.getMobileSpore();

		Destroyable socketHandler = console.openSocket(session);
		session.socketHandle = socketHandler;
		
		InetSocketAddress sockAddr = (InetSocketAddress)fget(session.bindAddress);
		CallbackSporePlanter planter = new CallbackSporePlanter(spore, sockAddr.getHostName(), sockAddr.getPort());
		byte[] binspore = serialize(planter);
		session.binspore = binspore;
		
		String javaCmd = ctx.getJvmExecCmd();
		String classpath = buildClasspath(console, ctx.getJvmClasspath());
		
		List<String> commands = new ArrayList<String>();
		commands.add(javaCmd);
		commands.add("-jar");
		commands.add("\"" + classpath +"\"");
		commands.add("-D" + ZLogFactory.PROP_ZLOG_MODE + "=slf4j");
		
		console.startProcess(".", commands.toArray(new String[0]), null, session);
		
		return session;
	}

	private String buildClasspath(HostControlConsole console, List<ClasspathEntry> jvmClasspath) {
		
		List<String> paths = console.cacheFiles(jvmClasspath);
		
		StringBuilder remoteClasspath = new StringBuilder();
		for(String path: paths) {
			if (remoteClasspath.length() > 0) {
				remoteClasspath.append(' ');
			}
			remoteClasspath.append(path);			
		}
		
		Manifest mf = new Manifest();
		mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mf.getMainAttributes().put(Attributes.Name.CLASS_PATH, remoteClasspath.toString());
		mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, SmartBootstraper.class.getName());
		
		byte[] booter;
		try {
			booter = ClasspathUtils.createManifestJar(mf);
		} catch (IOException e) {
			throw new RuntimeException();
		}
		FileBlob bb = Classpath.createBinaryEntry("booter.jar", booter);
		
		String path = console.cacheFile(bb);
		
		return path;
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
	
	// FIXME shutdown sequence is a mess, should be handled properly
	private static class ControlledSession implements ManagedProcess, ProcessHandler, SocketHandler {

		RemoteExecutionSession session;
		FutureBox<SocketAddress> bindAddress = new FutureBox<SocketAddress>();
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
			session.setTransportConnection(new NamedStreamPair("tunnel(" + remoteHost + ":" + remotePort + ")", soIn, soOut));
			executor.setData(session.getRemoteExecutor());
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
			session.terminate();
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
			session.terminate();
			if (procHandle != null) {
				procHandle.destroy();
			}
		}

		@Override
		public FutureEx<Integer> getExitCodeFuture() {
			return exitCode;
		}		
	}	
}
