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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Tunneller extends TunnellerIO {
	
	public static void main(String[] args) {
		// use std out for binary communications
		// use std err for console diagnostic 
		InputStream input = System.in;
		PrintStream output = System.out;
		System.setOut(System.err);
		
		new Tunneller().process(input, output);		
	}

	private DataInputStream ctrlReq;
	private DataOutputStream ctrlRep;

	private Map<Long, ProcessHandler> processes = new ConcurrentHashMap<Long, ProcessHandler>();
	private NavigableMap<Long, ServerSocket> sockets = new TreeMap<Long, ServerSocket>();
	
	public Tunneller() {
		super("", System.out);
	}
	
	public void process(InputStream input, OutputStream output) {
				
		Channel ctrlIn = new Channel(CTRL_REQ, Direction.INBOUND, 4 << 10);
		Channel ctrlOut = new Channel(CTRL_REP, Direction.OUTBOUND, 4 << 10);

		addChannel(ctrlIn);
		addChannel(ctrlOut);
		
		ctrlReq = new DataInputStream(ctrlIn.inbound);
		ctrlRep = new DataOutputStream(ctrlOut.outbound);

		
		OutboundMux out = new OutboundMux(output);
		out.start();

		try {
			readMagic(input);
		} catch (IOException e) {
			diagOut.println("Failed to init stream. " + e.toString());
		}
		InboundDemux in = new InboundDemux(input);
		in.start();

		diagOut.println("Tunneller started");
		processCommands();
		in.interrupt();
		out.interrupt();
	}

	private void processCommands() {
		try {
			while(true) {
				int cmd = ctrlReq.readInt();
				switch(cmd) {
					case ExecCmd.ID: processExec(); break;
					case KillCmd.ID: processKill(); break;
					case BindCmd.ID: processBind(); break;
					case AcceptCmd.ID: processAccept(); break;
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

	private void processExec() throws IOException {
		
		ExecCmd cmd = new ExecCmd();
		cmd.read(ctrlReq);

		Channel stdIn = new Channel(cmd.inId, Direction.INBOUND, 16 << 10); 
		Channel stdOut = new Channel(cmd.outId, Direction.OUTBOUND, 16 << 10); 
		Channel stdErr = new Channel(cmd.errId, Direction.OUTBOUND, 16 << 10); 
		addChannel(stdIn);
		addChannel(stdOut);
		addChannel(stdErr);
		
		startProc(cmd.procId, cmd.workingDir, cmd.command, cmd.env, stdIn.inbound, stdOut.outbound, stdErr.outbound);
	}

	private void processKill() throws IOException {
		
		KillCmd cmd = new KillCmd();
		cmd.read(ctrlReq);

		ProcessHandler ph = processes.get(cmd.procId);
		if (ph != null) {
			ph.proc.destroy();
		}
	}

	private void processBind() throws IOException {
		
		BindCmd cmd = new BindCmd();
		cmd.read(ctrlReq);
		
		ServerSocket ss = new ServerSocket();
		ss.bind(new InetSocketAddress("127.0.0.1", 0));
		sockets.put(cmd.sockId, ss);
		InetSocketAddress sa = (InetSocketAddress) ss.getLocalSocketAddress();
		
		sendBound(cmd.sockId, sa.getHostName(), sa.getPort());		
	}

	private void processAccept() throws IOException {
		AcceptCmd cmd = new AcceptCmd();
		cmd.read(ctrlReq);
		
		Channel soIn = new Channel(cmd.inId, Direction.INBOUND, 16 << 10); 
		Channel soOut = new Channel(cmd.outId, Direction.OUTBOUND, 16 << 10); 
		addChannel(soIn);
		addChannel(soOut);
		
		startAcceptor(cmd.cmdId, sockets.get(cmd.sockId), soIn.inbound, soOut.outbound);}
	
	private void startProc(long procId, String workingDir, String command[],	String[] env, InputStream stdIn, OutputStream stdOut, OutputStream stdErr) {
		try {
			File wd = new File(workingDir).getCanonicalFile();
			Process process = Runtime.getRuntime().exec(command, env, wd);
			new ProcessHandler(procId, process, stdIn, stdOut, stdErr).start();
			sendStarted(procId);
		} catch (IOException e) {
			PrintStream ps = new PrintStream(stdErr);
			e.printStackTrace(ps);
			ps.flush();
			close(stdIn);
			close(stdOut);
			close(stdErr);
			sendExitCode(procId, -1);
		}
	}

	private void startAcceptor(long cmdId, ServerSocket serverSocket, InputStream inbound, OutputStream outbound) {		
		new SocketHandler(serverSocket, cmdId, inbound, outbound).start();		
	}

	synchronized void sendStarted(long procId) {
		try {
			StartedCmd cmd = new StartedCmd();
			cmd.procId = procId;
			cmd.write(ctrlRep);
			writePending();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	synchronized void sendExitCode(long procId, int code) {
		try {
			ExitCodeCmd cmd = new ExitCodeCmd();
			cmd.procId = procId;
			cmd.code = code;
			cmd.write(ctrlRep);
			writePending();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	synchronized void sendBound(long sockId, String host, int port) {
		try {
			BoundCmd cmd = new BoundCmd();
			cmd.sockId = sockId;
			cmd.host = host;
			cmd.port = port;
			cmd.write(ctrlRep);
			writePending();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	synchronized void sendAccepted(long cmdId, String rhost, int rport) {
		try {
			AcceptedCmd cmd = new AcceptedCmd();
			cmd.cmdId = cmdId;
			cmd.remoteHost = rhost;
			cmd.remotePort = rport;
			cmd.write(ctrlRep);
			writePending();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	void close(Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}

	void close(Socket c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}
	
	boolean pump(InputStream is, OutputStream os) {
		try {
			if (eof(is)) {
				close(os);
				return false;
			}
			int n;
			try {
				n = is.available();
			} catch (IOException e) {
				return false;
			}
			if (n == 0) {
				return false; 
			}
			byte[] buf = new byte[n];
			n = is.read(buf);
			os.write(buf, 0, n);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	boolean eof(InputStream is) {
		try {
			return is.read(new byte[0]) < 0;
		} catch (IOException e) {
			return true;
		}
	}

	private class ProcessHandler extends Thread {

		final long procId;
		final Process proc;
		final InputStream stdIn;
		final OutputStream stdOut;
		final OutputStream stdErr;
		
		public ProcessHandler(long procId, Process process, InputStream stdIn,	OutputStream stdOut, OutputStream stdErr) {
			this.procId = procId;
			this.proc = process;
			this.stdIn = stdIn;
			this.stdOut = stdOut;
			this.stdErr = stdErr;
			setDaemon(true);
			setName("PROC[" + procId + "]");
			processes.put(procId, this);
		}
		
		@Override
		public void run() {
			try {
				while(true) {
					if (	pump(stdIn, proc.getOutputStream()) 
						  | pump(proc.getInputStream(), stdOut)
						  | pump(proc.getErrorStream(), stdErr)) {
						writePending();
						continue;
					}
					else {
						try {
							int ec = proc.exitValue();
							try {
								// give a chance for streams to catch up
								Thread.sleep(50);
							} catch (InterruptedException e) {
								// ignore
							}; 
							pump(proc.getInputStream(), stdOut);
							pump(proc.getErrorStream(), stdErr);
							
							close(stdOut);
							close(stdErr);
							
							proc.destroy();
							
							sendExitCode(procId, ec);
							diagOut.println("Process [" + procId + "] exit code: " + ec);
							break;
						}
						catch(IllegalThreadStateException e) {
							try {
								sleep(50);
							} catch (InterruptedException ee) {
								// ignore;
							}
							continue;
						}
					}
				}
			}
			finally {
				processes.remove(procId);
			}
		}
	}
		
	private class SocketHandler extends Thread {
		
		final ServerSocket socket;
		Socket sock;
		final long cmdId;
		final InputStream is;
		final OutputStream os;
		
		public SocketHandler(ServerSocket socket, long cmdId, InputStream is, OutputStream os) {
			this.socket = socket;
			this.cmdId = cmdId;
			this.is = is;
			this.os = os;
			setDaemon(true);
			setName("ACCEPT[" + socket.getLocalSocketAddress() + "]");
		}
		
		@Override
		public void run() {
			InputStream soIn;
			OutputStream soOut;
			String rhost;
			int rport;
			try {
				sock = socket.accept();
				sock.setKeepAlive(true);
				soIn = sock.getInputStream();
				soOut = sock.getOutputStream();
				InetSocketAddress saddr = (InetSocketAddress) sock.getRemoteSocketAddress();
				rhost = saddr.getHostName();
				rport = saddr.getPort();
			} catch (IOException e) {
				sendAccepted(cmdId, e.toString(), 0);
				close(is);
				close(os);
				close(sock);
				return;
			}
			sendAccepted(cmdId, rhost, rport);
			setName("CONNECTION[" + sock.getRemoteSocketAddress() + "]");

			
			while(sock.isConnected() && !sock.isClosed()) {
				if (	pump(soIn, os)
					 || pump(is, soOut)) {
					writePending();
					continue;
				}
				else {
					try {
						sleep(50);
					} catch (InterruptedException e) {
					}
				}
			}
			close(is); // TODO control side should close input
			close(os);
			writePending();
		}				
	}
	
}
