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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.gridkit.vicluster.telecontrol.StreamPipe;

class TunnellerIO {

	protected static final byte[] MAGIC = "TUNNELLER".getBytes();
	
	protected static final long CTRL_REQ = -1;
	protected static final long CTRL_REP = -2;

	private static final int CMD_EXEC = 1;
	private static final int CMD_STARTED = 2;
	private static final int CMD_KILL = 3;
	private static final int CMD_EXIT_CODE = 4;
	private static final int CMD_BIND = 5;
	private static final int CMD_BOUND = 6;
	private static final int CMD_ACCEPT = 7;
	private static final int CMD_ACCEPTED = 8;
	private static final int CMD_FILE_PUSH = 9;
	private static final int CMD_FILE_PUSH_RESPONSE = 10;
	
	enum Direction {INBOUND, OUTBOUND}

	static class ExecCmd {
		
		static final int ID = CMD_EXEC;
		
		long procId;
		long inId;
		long outId;
		long errId;
		
		String workingDir;
		String[] command;
		Map<String, String> env;
		
		public void read(DataInputStream dis) throws IOException {
			procId = dis.readLong();
			inId = dis.readLong();
			outId = dis.readLong();
			errId = dis.readLong();
			
			workingDir = dis.readUTF();
			command = readStringArray(dis);
			env = readStringMap(dis);
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(procId);
			dos.writeLong(inId);
			dos.writeLong(outId);
			dos.writeLong(errId);
			dos.writeUTF(workingDir);
			writeStringArray(dos, command);
			writeStringMap(dos, env);
		}
	}

	static class StartedCmd {
		
		static final int ID = CMD_STARTED;
		
		long procId;
		
		public void read(DataInputStream dis) throws IOException {
			procId = dis.readLong();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(procId);
		}
	}

	static class KillCmd {
		
		static final int ID = CMD_KILL;
		
		long procId;
		
		public void read(DataInputStream dis) throws IOException {
			procId = dis.readLong();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(procId);
		}
	}

	static class ExitCodeCmd {
		
		static final int ID = CMD_EXIT_CODE;
		
		long procId;
		int code;
		
		public void read(DataInputStream dis) throws IOException {
			procId = dis.readLong();
			code = dis.readInt();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(procId);
			dos.writeInt(code);
		}
	}

	static class BindCmd {
		
		static final int ID = CMD_BIND;
		
		long sockId;
		
		public void read(DataInputStream dis) throws IOException {
			sockId = dis.readLong();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(sockId);
		}
	}

	static class BoundCmd {
		
		static final int ID = CMD_BOUND;
		
		long sockId;
		String host;
		int port;
		
		public void read(DataInputStream dis) throws IOException {
			sockId = dis.readLong();
			host = dis.readUTF();
			port = dis.readInt();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(sockId);
			dos.writeUTF(host);
			dos.writeInt(port);
		}
	}
	
	static class AcceptCmd {
		
		static final int ID = CMD_ACCEPT;
		
		long sockId;
		long cmdId;
		long inId;
		long outId;
		
		public void read(DataInputStream dis) throws IOException {
			sockId = dis.readLong();
			cmdId = dis.readLong();
			inId = dis.readLong();
			outId = dis.readLong();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(sockId);
			dos.writeLong(cmdId);
			dos.writeLong(inId);
			dos.writeLong(outId);
		}
	}
	
	static class AcceptedCmd {
		
		static final int ID = CMD_ACCEPTED;
		
		long cmdId;
		String remoteHost;
		int remotePort;
		
		public void read(DataInputStream dis) throws IOException {
			cmdId = dis.readLong();
			remoteHost = dis.readUTF();
			remotePort = dis.readInt();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(cmdId);
			dos.writeUTF(remoteHost);
			dos.writeInt(remotePort);
		}
	}

	static class FilePushCmd {
		
		static final int ID = CMD_FILE_PUSH;
		
		long fileId;
		String path;
		long inId;
		
		public void read(DataInputStream dis) throws IOException {
			fileId = dis.readLong();
			path = dis.readUTF();
			inId = dis.readLong();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(fileId);
			dos.writeUTF(path);
			dos.writeLong(inId);
		}
	}

	static class FilePushResponseCmd {
		
		static final int ID = CMD_FILE_PUSH_RESPONSE;
		
		long fileId;
		String path;
		long size;
		String error;
		
		public void read(DataInputStream dis) throws IOException {
			fileId = dis.readLong();
			path = dis.readUTF();
			size = dis.readLong();
			error = dis.readUTF();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(fileId);
			dos.writeUTF(path);
			dos.writeLong(size);
			dos.writeUTF(error);
		}
	}

	private static String[] readStringArray(DataInputStream dis) throws IOException {
		int n = dis.readShort();
		String[] command = new String[n];
		for(int i = 0; i != command.length; ++i) {
			command[i] = dis.readUTF();
		}
		return command;
	}

	private static void writeStringArray(DataOutputStream dos, String[] strings) throws IOException {
		dos.writeShort(strings.length);
		for(String string: strings) {
			dos.writeUTF(string);
		}
	}

	private static Map<String, String> readStringMap(DataInputStream dis) throws IOException {
		int n = dis.readShort();
		if (n == -1) {
			return null;
		}
		Map<String, String> result = new LinkedHashMap<String, String>(n);
		for(int i = 0; i != n; ++i) {
			String key = dis.readUTF();
			boolean notNull = dis.readBoolean();
			String value;
			if (notNull) {
				value = dis.readUTF();
			}
			else {
				value = null;				
			}
			result.put(key, value);
		}
		return result;
	}

	private static void writeStringMap(DataOutputStream dos, Map<String, String> map) throws IOException {
		if (map == null) {
			dos.writeShort(-1);
		}
		else {
			dos.writeShort(map.size());
			for(String key: map.keySet()) {
				dos.writeUTF(key);
				String val = map.get(key);
				if (val == null) {
					dos.writeBoolean(false);
				}
				else {
					dos.writeBoolean(true);
					dos.writeUTF(val);
				}
			}
		}
	}
	
	protected boolean traceExec;
	protected boolean traceExitCode;
	protected boolean traceBind;
	protected boolean traceAccept;
	protected boolean traceChannelOpen;
	protected boolean traceChannelData;
	protected boolean traceChannelClose;
	
	protected boolean embededMode = true;
	
	protected PrintStream diagOut;
	
	private String threadSuffix;
	private NavigableMap<Long, Channel> channels = new TreeMap<Long, Channel>();
	private Semaphore writePending = new Semaphore(0); 

	protected TunnellerIO(String name, PrintStream diagOut) {
		this.threadSuffix = name;
		this.diagOut = diagOut;
	}

	protected void readMagic(InputStream is) throws IOException {
		byte[] data = new byte[MAGIC.length];
		int n = 0;
		while(n < data.length) {
			int m = is.read(data, n, data.length - n);
			if (m < -1) {
				throw new IOException("Failed to read MAGIC, EOF reached");
			}
			n += m;
		}
		if (!Arrays.equals(data, MAGIC)) {
			int x = is.available();
			if (x > 0) {
				byte[] buf = Arrays.copyOf(data, data.length + x);
				is.read(buf, n, x);
				data = buf;
			}
			
			throw new IOException("Magic not match, expected [" + new String(MAGIC) + "], read [" + new String(data) + "]");
		}
	}
	
	protected void writePending() {
		writePending.release(1);
	}
	
	protected void addChannel(Channel ch) {
		synchronized(channels) {
			if (channels.containsKey(ch.channelId)) {
				throw new IllegalArgumentException("Channel already exists: " + ch.channelId);
			}
			channels.put(ch.channelId, ch);
			if (traceChannelOpen) {
				diagOut.println("Channel open: [" + ch.channelId + "] " + ch.direction);
			}
		}
	}
	
	protected synchronized void stopChannels() {
		synchronized(channels) {
			for(Channel ch: channels.values()) {
				close(ch.inbound);
				close(ch.outbound);
			}		
		}
	}				
	
	private void close(Closeable c) {
		try {
			c.close();
		} catch (IOException e) {
			// ignore
		}
	}


	protected class OutboundMux extends Thread {

		private DataOutputStream out;
		private long nextInQueue; 
		
		public OutboundMux(OutputStream os) {
			this.out = new DataOutputStream(os);
			setDaemon(true);
		}
		
		@Override
		public void interrupt() {
			super.interrupt();
			try {
				out.close();
			} catch (IOException e) {
				// ignore;
			}
		}
		
		@Override
		public void run() {
			setName("OutboundMux" + threadSuffix);
			try {
				out.write(MAGIC);
				out.flush();
				byte[] buf = new byte[1024];
				while(true) {
					writePending.tryAcquire(100, TimeUnit.MILLISECONDS);
					writePending.drainPermits();
					nextInQueue = Long.MIN_VALUE;
					while(true) {
						Channel ch = nextWritePending();
						if (ch == null) {
							break;
						}
						int n;
						try {
							n = ch.inbound.read(buf);
						} catch (IOException e) {
							// can only happen if channel has been closed
							throw new InterruptedException("Termination condition detected");
						}
						try {
							if (n < 0) {
								if (traceChannelClose) {
									diagOut.println("Channel closed: [" + ch.channelId + "] " + ch.direction);
								}
								synchronized(channels) {
									channels.remove(ch.channelId);
								}
								out.writeLong(ch.channelId);
								out.writeShort(0); // EOF marker
								if (traceChannelData) {
									diagOut.println("Channel send: [" + ch.channelId + "] - EOF");
								}
							}
							else {
								out.writeLong(ch.channelId);
								out.writeShort(n);
								out.write(buf, 0, align(n));
								if (traceChannelData) {
									diagOut.println("Channel send: [" + ch.channelId + "] " + n + " bytes");
								}

							}
						} catch (IOException e) {
							diagOut.println("Outbound write failed: " + e.toString());
						}
					}
					try {
						out.flush();
					} catch (IOException e) {
						diagOut.println("Outbound write failed: " + e.toString());
					}
				}
			} catch (InterruptedException e) {
				if (!embededMode) {
					diagOut.println("Outbound mux stopped.");
				}
			} catch (IOException e) {
				diagOut.println("Outbound write failed: " + e.toString());
				diagOut.println("Outbound mux stopped");
			}
		}
		
		private Channel nextWritePending() {
			int n = 0;
			synchronized(channels) {
				while(n <= channels.size()) {
					Entry<Long, Channel> ne = channels.ceilingEntry(nextInQueue);
					if (ne == null) {
						nextInQueue = Long.MIN_VALUE;
						continue;
					}
					Channel ch = ne.getValue();
					nextInQueue = ch.channelId + 1;
					++n;
					try {
						if (ch.direction == Direction.OUTBOUND && ch.inbound.available() > 0) {
							return ch;
						}
					} catch (IOException e) {
						return ch;
					}
				}
			}
			return null;
		}		
	}

	protected class InboundDemux extends Thread {

		protected DataInputStream in;
		
		public InboundDemux(InputStream in) {
			this.in = new DataInputStream(in);
			setName("InboundDemux" + threadSuffix);
			setDaemon(true);
		}
		
		@Override
		public void interrupt() {
			super.interrupt();
			try {
				in.close();
			} catch (IOException e) {
				// ignore;
			}
		}
		
		@Override
		public void run() {
			try {
				while(true) {
					long chId = in.readLong();
					int size = in.readShort();
					int asize = align(size);
					byte[] buf = new byte[asize];
					in.readFully(buf);
					if (traceChannelData) {
						diagOut.println("Channel received: [" + chId + "] " + (size == 0 ? "EOF" : size + " bytes"));
					}
					Channel ch;
					synchronized(channels) {
						ch = channels.get(chId);
					}
					if (ch == null) {
						diagOut.println("WARN: Channel " + chId + " do not exists");
					}
					else if (ch.direction == Direction.OUTBOUND) {
						diagOut.println("WARN: Inbound packet to outbound channel " + chId);
					}
					else {
						try {
							if (size == 0) {
								if (traceChannelClose) {
									diagOut.println("Channel closed: [" + ch.channelId + "] " + ch.direction);
								}
								ch.outbound.close();
								synchronized(channels) {
									channels.remove(ch.channelId);
								}							
							}
							else {
								ch.outbound.write(buf, 0, size);
							}
						}
						catch(IOException e) {
							// closed by reader
							synchronized(channels) {
								channels.remove(ch.channelId);
							}							
						}
					}
				}
			} catch (IOException e) {
				if (!embededMode) {
					e.printStackTrace();
					diagOut.println("Inbound mux stopped");
				}
				stopChannels();
			}
		}
	}
	
	private int align(int size) {
		return (size + 7) & (~7);
	}
	
	protected static class Channel {
		
		final long channelId;
		final Direction direction;
		final InputStream inbound;
		final OutputStream outbound;	
		
		public Channel(long id, Direction direction, int bufferSize) {
			this.channelId = id;
			this.direction = direction;
			StreamPipe pipe = new StreamPipe(bufferSize);
			this.inbound = pipe.getInputStream();
			this.outbound = pipe.getOutputStream();
		}

		public Channel(long id, OutputStream os) {
			this.channelId = id;
			this.direction = Direction.INBOUND;
			this.inbound = null;
			this.outbound = os;
		}

		public Channel(long id, InputStream is) {
			this.channelId = id;
			this.direction = Direction.OUTBOUND;
			this.inbound = is;
			this.outbound = null;
		}
	}	
}
