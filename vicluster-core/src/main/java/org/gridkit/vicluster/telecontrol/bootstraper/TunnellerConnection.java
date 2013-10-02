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

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.util.concurrent.FutureBox;

public class TunnellerConnection extends TunnellerIO {

	private InboundDemux inbound;
	private OutboundMux outbound;
	private Control control;
	
	private long nextChannel = 0;
	private long nextProc = 0;
	private long nextSocket = 0;
	private long nextAccept = 0;
	private long nextFile = 0;
	
	private DataOutputStream ctrlReq;
	private DataInputStream ctrlRep;

	private Map<Long, ExecContext> execs = new HashMap<Long, ExecContext>();
	private Map<Long, SocketContext> socks = new HashMap<Long, SocketContext>();
	private Map<Long, AcceptContext> accepts = new HashMap<Long, AcceptContext>();
	private Map<Long, FileContext> files = new HashMap<Long, FileContext>();
	
	private FutureBox<Void> magicReceived = new FutureBox<Void>();
	private boolean terminated;
	
	public TunnellerConnection(String name, InputStream is, OutputStream os, PrintStream diagOut, long connTimeout, TimeUnit tu) throws IOException, InterruptedException, TimeoutException {
		super(":" + name, diagOut);
		
		embededMode = true;
		
		Channel rq = new Channel(CTRL_REQ, Direction.OUTBOUND, 4 << 10);
		Channel rp = new Channel(CTRL_REP, Direction.INBOUND, 4 << 10);
		
		addChannel(rq);
		addChannel(rp);
		
		ctrlReq = new DataOutputStream(rq.outbound);
		ctrlRep = new DataInputStream(rp.inbound);
		
		outbound = new OutboundMux(os);
		outbound.start();
		
		inbound = new InboundDemux(is) {
			public void run() {
				try {
					readMagic(in);
					magicReceived.setData(null);
				}
				catch(IOException e) {
					magicReceived.setError(e);
				}
				super.run();
			};
		};
		inbound.start();

		try {
			magicReceived.get(connTimeout, tu);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw ((IOException)e.getCause());
			}
			else {
				throw new IOException(e.getCause());
			}
		}
		
		control = new Control(name);
		control.start();		
	}

	public synchronized long newSocket(SocketHandler handler) throws IOException {
		long sockId = nextSocket++;
		SocketContext ctx = new SocketContext();
		ctx.sockId = sockId;
		ctx.handler = handler;
		socks.put(sockId, ctx);
		try {
			sendBind(sockId);
		} catch (IOException e) {
			shutdown();
			throw new IOException("Broken tunnel");
		}
		return sockId;
	}

	public synchronized void pushFile(String path, FileHandler handler) throws IOException {
		long fileId = nextFile++;
		FileContext ctx = new FileContext();
		ctx.chanId = newChannelId();
		ctx.handler = handler;
		files.put(fileId, ctx);
		try {
			sendPush(fileId, path, ctx.chanId);
		} catch (IOException e) {
			shutdown();
			throw new IOException("Broken tunnel");
		}
	}
	
	public synchronized long exec(String wd, String[] cmd, String[] env, ExecHandler handler) throws IOException {
		long procId = nextProc++;
		ExecContext ctx = new ExecContext();
		ctx.procId = procId;
		ctx.handler = handler;
		
		long stdIn = newChannelId();
		long stdOut = newChannelId();
		long stdErr = newChannelId();
		
		ctx.stdIn = newOutbound(stdIn);
		ctx.stdOut = newInbound(stdOut);
		ctx.stdErr = newInbound(stdErr);
		
		execs.put(ctx.procId, ctx);
		
		try {
			sendExec(procId, wd, cmd, env, stdIn, stdOut, stdErr);
			return procId;
		} catch (IOException e) {
			shutdown();
			throw new IOException("Broken tunnel");
		}		
	}
	
	public synchronized void kill(long execId) throws IOException {
		if (execs.containsKey(execId)) {
		
			try {
				sendKill(execId);
			} catch (IOException e) {
				shutdown();
				throw new IOException("Broken tunnel");
			}
		}
	}
	
	public void close() {
		shutdown();
	}
	
	private void shutdown() {
		close(ctrlRep);
		close(ctrlReq);
		
		terminated = true;
		inbound.interrupt();
		outbound.interrupt();
		control.interrupt();
		
		join(inbound);
		join(outbound);
		join(control);		
	}
	
	private void close(Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (IOException e) {
			// do nothing
		}
	}

	private void join(Thread p) {
		try {
			if (p != null && p != Thread.currentThread()) {
				p.join();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			// do nothing
		}
	}
	

	private synchronized void addAcceptor(SocketContext context) throws IOException {
		AcceptContext ac = new AcceptContext();
		ac.cmdId = nextAccept++;
		ac.context = context;
		
		long inId = newChannelId();
		long outId = newChannelId();
		ac.soIn = newInbound(inId);
		ac.soOut = newOutbound(outId);
		
		accepts.put(ac.cmdId, ac);
		
		sendAccept(ac.context.sockId, ac.cmdId,inId, outId);		
	}
	
	private synchronized void sendExec(long procId, String wd, String[] command, String[] env, long stdIn, long stdOut, long stdErr) throws IOException {
		ExecCmd cmd = new ExecCmd();
		cmd.procId = procId;
		cmd.workingDir = wd;
		cmd.command = command;
		cmd.env = env;
		cmd.inId = stdIn;
		cmd.outId = stdOut;
		cmd.errId = stdErr;
		
		cmd.write(ctrlReq);
	}

	private synchronized void sendAccept(long sockId, long cmdId, long inId, long outId) throws IOException {
		AcceptCmd cmd = new AcceptCmd();
		cmd.sockId = sockId;
		cmd.cmdId = cmdId;
		cmd.inId = outId; // crossing channels
		cmd.outId = inId; // 
		
		cmd.write(ctrlReq);
	}

	private synchronized void sendBind(long sockId) throws IOException {
		BindCmd cmd = new BindCmd();
		cmd.sockId = sockId;
		
		cmd.write(ctrlReq);
	}

	private synchronized void sendPush(long fileId, String path, long outId) throws IOException {
		FilePushCmd cmd = new FilePushCmd();
		cmd.fileId = fileId;
		cmd.path = path;
		cmd.inId = outId;
		
		cmd.write(ctrlReq);
	}

	private synchronized void sendKill(long procId) throws IOException {
		KillCmd cmd = new KillCmd();
		cmd.procId = procId;
		
		cmd.write(ctrlReq);
	}

	private InputStream newInbound(long id) {
		Channel ch = new Channel(id, Direction.INBOUND, 16 << 10);
		addChannel(ch);
		return ch.inbound;
	}

	private OutputStream newOutbound(long id) {
		Channel ch = new Channel(id, Direction.OUTBOUND, 16 << 10);
		addChannel(ch);
		return new NotifyingOutputStream(ch.outbound);
	}


	private synchronized long newChannelId() {
		return nextChannel++;
	}

	private static class ExecContext {
		
		long procId;
		ExecHandler handler;
		
		OutputStream stdIn;
		InputStream stdOut;
		InputStream stdErr;		
		
	}

	private static class SocketContext {
		
		long sockId;
		SocketHandler handler;

		String hostname;
		int port;		
	}

	private static class AcceptContext {
		
		long cmdId;
		SocketContext context;

		InputStream soIn;
		OutputStream soOut;
	}

	private static class FileContext {

		String rpath;
		FileHandler handler;
		long chanId;
		OutputStream channel;		
	}
	
	private class Control extends Thread {
		
		public Control(String name) {
			setName("TunnelControl:" + name);
		}
		
		@Override
		public void run() {
			try {
				try {
					while(!terminated) {
						int cmd = ctrlRep.readInt();
						switch(cmd) {
							case StartedCmd.ID: processStarted(); break;
							case ExitCodeCmd.ID: processExitCode(); break;
							case BoundCmd.ID: processBound(); break;
							case AcceptedCmd.ID: processAccepted(); break;
							case FilePushResponseCmd.ID: processFileResponse(); break;
							default:
								System.out.println("ERROR: Unexpected command: " + cmd);
								break;
						}
					}		
				} catch (IOException e) {
					diagOut.println("Control thread stopped");
				} catch (Exception e) {
					diagOut.println("Error in control thread: " + e.toString());
				}
			}
			finally {
				shutdown();
			}
		}

		private void processAccepted() throws IOException {
			AcceptedCmd cmd = new AcceptedCmd();
			cmd.read(ctrlRep);
			
			AcceptContext ctx;
			synchronized(TunnellerConnection.this) {
				
				ctx = accepts.remove(cmd.cmdId);
				if (ctx == null) {
					throw new RuntimeException("Unknown acceptor ID: " + cmd.cmdId);
				}
				addAcceptor(ctx.context);
			}			
			ctx.context.handler.accepted(cmd.remoteHost, cmd.remotePort, ctx.soIn, ctx.soOut);
		}

		private void processFileResponse() throws IOException {
			FilePushResponseCmd cmd = new FilePushResponseCmd();
			cmd.read(ctrlRep);
			
			if (cmd.error.length() == 0 && cmd.size == -1) {
				// request accepted
				FileContext ctx;
				synchronized(TunnellerConnection.this) {
					ctx = files.get(cmd.fileId);
					if (ctx == null) {
						throw new RuntimeException("Unknown file ID: " + cmd.fileId);
					}
				}
				ctx.rpath = cmd.path;
				ctx.channel = newOutbound(ctx.chanId);
				ctx.handler.accepted(ctx.channel);
			}
			else {
				FileContext ctx;
				synchronized(TunnellerConnection.this) {
					ctx = files.remove(cmd.fileId);
					if (ctx == null) {
						throw new RuntimeException("Unknown file ID: " + cmd.fileId);
					}
				}
				ctx.rpath = cmd.path;
				close(ctx.channel);
				
				if (cmd.error.length() != 0) {
					ctx.handler.failed(ctx.rpath, cmd.error);
				}
				else {
					ctx.handler.confirmed(ctx.rpath, cmd.size);
				}
			}
		}

		private void processBound() throws IOException {
			BoundCmd cmd = new BoundCmd();
			cmd.read(ctrlRep);
			
			SocketContext ctx;
			synchronized(TunnellerConnection.this) {
				ctx = socks.get(cmd.sockId);
				if (ctx == null) {
					throw new RuntimeException("Unknown socket ID: " + cmd.sockId);
				}
				ctx.hostname = cmd.host;
				ctx.port = cmd.port;
				addAcceptor(ctx);
				addAcceptor(ctx);
			}			

			ctx.handler.bound(ctx.hostname, ctx.port);
		}

		private void processStarted() throws IOException {
			StartedCmd cmd = new StartedCmd();
			cmd.read(ctrlRep);
			
			ExecContext ctx;
			synchronized(TunnellerConnection.this) {
				ctx = execs.get(cmd.procId);
				if (ctx == null) {
					throw new RuntimeException("Unknown exec ID: " + cmd.procId);
				}
			}
			
			ctx.handler.started(ctx.stdIn, ctx.stdOut, ctx.stdErr);
		}		

		private void processExitCode() throws IOException {
			ExitCodeCmd cmd = new ExitCodeCmd();
			cmd.read(ctrlRep);
			
			ExecContext ctx;
			synchronized(TunnellerConnection.this) {
				ctx = execs.remove(cmd.procId);
				if (ctx == null) {
					throw new RuntimeException("Unknown exec ID: " + cmd.procId);
				}
			}
			
			ctx.handler.finished(cmd.code);
		}		
	}
	
	public interface ExecHandler {
		
		public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr);

		public void finished(int exitCode);
		
	}

	public interface FileHandler {
		
		public void accepted(OutputStream out);
		
		public void confirmed(String path, long size);

		public void failed(String path, String error);
		
	}

	public interface SocketHandler {
		
		public void bound(String host, int port);
		
		public void accepted(String remoteHost, int remotePort, InputStream soIn, OutputStream soOut);
	}
	
	private class NotifyingOutputStream extends OutputStream {
		
		private final OutputStream delegate;

		public NotifyingOutputStream(OutputStream delegate) {
			this.delegate = delegate;
		}

		public void write(int b) throws IOException {
			delegate.write(b);
			writePending();
		}

		public void write(byte[] b) throws IOException {
			delegate.write(b);
			writePending();
		}

		public void write(byte[] b, int off, int len) throws IOException {
			delegate.write(b, off, len);
			writePending();
		}

		public void flush() throws IOException {
			delegate.flush();
		}

		public void close() throws IOException {
			delegate.close();
			writePending();
		}

		public String toString() {
			return delegate.toString();
		}
	}
}
