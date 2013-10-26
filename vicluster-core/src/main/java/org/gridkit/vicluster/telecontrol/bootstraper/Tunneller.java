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
import java.io.FileOutputStream;
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

		if (traceControlThread) {
			diagOut.println("Tunneller started");
		}
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
					case FilePushCmd.ID: processPushFile(); break;
					default:
						System.out.println("ERROR: Unexpected command: " + cmd);
						break;
				}
			}		
		} catch (IOException e) {
			if (traceControlThread) {
				diagOut.println("Control thread stopped");
			}
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
		
		startAcceptor(cmd.cmdId, sockets.get(cmd.sockId), soIn.inbound, soOut.outbound);
	}

	private void processPushFile() throws IOException {
		FilePushCmd cmd = new FilePushCmd();
		cmd.read(ctrlReq);
		
		String error = "";
		long size = -1;
		
		String path = cmd.path;
		try {
			path = transformPath(path);
			File file = new File(path);
			if (file.exists()) {
				size = file.length();
				if (file.isDirectory()) {
					error = "Target path is directory";
				}
			}
			else {
				FileWriter writer = new FileWriter(cmd.fileId, path);
				Channel soIn = new Channel(cmd.inId, Direction.INBOUND, 16 << 10); 
				addChannel(soIn);
				writer.in = soIn.inbound;
				writer.start();			
			}
		}
		catch(IOException e) {
			error = e.toString();
		}
		
		sendFileResponse(cmd.fileId, path, size, error);
	}
	
	private void startProc(long procId, String workingDir, String command[],	Map<String, String> env, InputStream stdIn, OutputStream stdOut, OutputStream stdErr) {
		try {
			File wd = new File(workingDir).getCanonicalFile();
			String[] envp = SystemHelper.buildInheritedEnvironment(env);
			Process process = Runtime.getRuntime().exec(command, envp, wd);
			new ProcessHandler(procId, process, stdIn, stdOut, stdErr).start();
			sendStarted(procId);
		} catch (IOException e) {
			PrintStream ps = new PrintStream(stdErr);
			e.printStackTrace(ps);
			ps.flush();
			close(stdIn);
			close(stdOut);
			close(stdErr);
			sendExitCode(procId, Integer.MIN_VALUE);
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

	synchronized void sendFileResponse(long fileId, String path, long size, String error) {
		try {
			FilePushResponseCmd cmd = new FilePushResponseCmd();
			cmd.fileId = fileId;
			cmd.path = path;
			cmd.size = size;
			cmd.error = error;
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
	
	boolean pump(String diag, InputStream is, OutputStream os) {
		try {
			if (eof(is)) {
				if (diag != null) {
					diagOut.println("Pump [" + diag + "]: EOF");
				}
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
			if (diag != null) {
				diagOut.println("Pump [" + diag + "]: " + n + " bytes");
			}
			return true;
		} catch (IOException e) {
			diagOut.println("Pump failure: " + e.toString());
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

	private static String transformPath(String path) throws IOException {
		if (path.startsWith("~/")) {
			String home = System.getProperty("user.home");
			File fp = new File(new File(home), path.substring("~/".length()));
			return fp.getCanonicalPath();
		}
		else if (path.startsWith("{tmp}/")) {
			File tmp = File.createTempFile("mark", "").getAbsoluteFile();
			tmp.delete();
			File fp = new File(tmp.getParentFile(), path.substring("{tmp}/".length()));
			return fp.getCanonicalPath();
		}
		else {
			return new File(path).getCanonicalPath();
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
				String dStdIn = traceProcIO ? "stdIn@" + procId : null;
				String dStdOut = traceProcIO ? "stdOut@" + procId : null;
				String dStdErr = traceProcIO ? "stdErr@" + procId : null;
				while(true) {
					if (	pump(dStdIn, stdIn, proc.getOutputStream()) 
						  | pump(dStdOut, proc.getInputStream(), stdOut)
						  | pump(dStdErr, proc.getErrorStream(), stdErr)) {
						// TODO separate IN/OUT
						try {
							proc.getOutputStream().flush();
						} catch (IOException e) {
							// Ignore
						}
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
							pump(dStdOut, proc.getInputStream(), stdOut);
							pump(dStdErr, proc.getErrorStream(), stdErr);
							
							close(stdOut);
							close(stdErr);
							
							proc.destroy();
							
							sendExitCode(procId, ec);
							if (traceExitCode) {
								diagOut.println("Process [" + procId + "] exit code: " + ec);
							}
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
				if (	pump(null, soIn, os)
					 || pump(null, is, soOut)) {
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

	private class FileWriter extends Thread {
		
		final long fileId;
		final File targetFile;
		final File tempFile;
		final FileOutputStream fos;
		InputStream in;
		
		
		public FileWriter(long fileId, String path) throws IOException {
			this.fileId = fileId;
			targetFile = new File(path);
			if (targetFile.getParentFile() != null) {
				targetFile.getParentFile().mkdirs();
			}
			
			tempFile = File.createTempFile(targetFile.getName() + ".", "", targetFile.getParentFile());
			fos = new FileOutputStream(tempFile);
			
			setDaemon(true);
			setName("FILE[" + path + "]");
		}
		
		@Override
		public void run() {
			byte[] buffer = new byte[8 << 10];
			try {
				while(true) {
					int n = in.read(buffer);
					if (n < 0) {
						break;
					}
					else {
						fos.write(buffer, 0, n);
					}
				}
				close(fos);
				close(in);
				if (!targetFile.exists()) {
					tempFile.renameTo(targetFile);
				}
				if (tempFile.exists()) {
					tempFile.delete();
				}
			} catch (IOException e) {
				close(fos);
				close(in);
				sendFileResponse(fileId, targetFile.getPath(), -1, e.toString());
				return;
			}
			if (targetFile.exists() && !targetFile.isDirectory()) {
				sendFileResponse(fileId, targetFile.getPath(), targetFile.length(), "");
			}
			else {
				sendFileResponse(fileId, targetFile.getPath(), -1, "Failed to rename target file");
			}
		}				
	}	
}
