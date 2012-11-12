package org.gridkit.vicluster.telecontrol.bootstraper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.gridkit.vicluster.telecontrol.StreamPipe;

class TunnellerIO {

	protected static final long CTRL_REQ = -1;
	protected static final long CTRL_REP = -2;

	private static final int CMD_EXEC = 1;
	private static final int CMD_STARTED = 2;
	private static final int CMD_EXIT_CODE = 3;
	private static final int CMD_BIND = 4;
	private static final int CMD_BOUND = 5;
	private static final int CMD_ACCEPT = 6;
	private static final int CMD_ACCEPTED = 7;
	
	enum Direction {INBOUND, OUTBOUND}

	static class ExecCmd {
		
		static final int ID = CMD_EXEC;
		
		long procId;
		long inId;
		long outId;
		long errId;
		
		String workingDir;
		String[] command;
		String[] env;
		
		public void read(DataInputStream dis) throws IOException {
			procId = dis.readLong();
			inId = dis.readLong();
			outId = dis.readLong();
			errId = dis.readLong();
			
			workingDir = dis.readUTF();
			command = readStringArray(dis);
			env = readStringArray(dis);
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(procId);
			dos.writeLong(inId);
			dos.writeLong(outId);
			dos.writeLong(errId);
			dos.writeUTF(workingDir);
			writeStringArray(dos, command);
			writeStringArray(dos, env);
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
		
		public void read(DataInputStream dis) throws IOException {
			cmdId = dis.readLong();
		}
		
		public void write(DataOutputStream dos) throws IOException {
			dos.writeInt(ID);
			dos.writeLong(cmdId);
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
	
	protected boolean traceExec;
	protected boolean traceExitCode;
	protected boolean traceBind;
	protected boolean traceAccept;
	protected boolean traceChannelOpen;
	protected boolean traceChannelData;
	protected boolean traceChannelClose;
	
	private String threadSuffix;
	private NavigableMap<Long, Channel> channels = new TreeMap<Long, Channel>();
	private Semaphore writePending = new Semaphore(0); 

	protected TunnellerIO(String name) {
		this.threadSuffix = name;
	}

	protected void writePending() {
		writePending.release(1);
	}
	
	protected synchronized void addChannel(Channel ch) {
		if (channels.containsKey(ch.channelId)) {
			throw new IllegalArgumentException("Channel already exists: " + ch.channelId);
		}
		channels.put(ch.channelId, ch);
		if (traceChannelOpen) {
			System.out.println("Channel open: [" + ch.channelId + "] " + ch.direction);
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
							throw new Error("Should never happen");
						}
						try {
							if (n < 0) {
								if (traceChannelClose) {
									System.out.println("Channel closed: [" + ch.channelId + "] " + ch.direction);
								}
								synchronized(channels) {
									channels.remove(ch.channelId);
								}
								out.writeLong(ch.channelId);
								out.writeShort(0); // EOF marker
								if (traceChannelData) {
									System.out.println("Channel send: [" + ch.channelId + "] - EOF");
								}
							}
							else {
								out.writeLong(ch.channelId);
								out.writeShort(n);
								out.write(buf, 0, align(n));
								if (traceChannelData) {
									System.out.println("Channel send: [" + ch.channelId + "] " + n + " bytes");
								}

							}
						} catch (IOException e) {
							System.out.println("Outbound write failed: " + e.toString());
						}
					}			
				}
			} catch (InterruptedException e) {
				System.out.println("Outbound mux stopped.");
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

		private DataInputStream in;
		
		public InboundDemux(InputStream in) {
			this.in = new DataInputStream(in);
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
			setName("InboundDemux" + threadSuffix);
			try {
				while(true) {
					long chId = in.readLong();
					int size = in.readShort();
					int asize = align(size);
					byte[] buf = new byte[asize];
					in.readFully(buf);
					if (traceChannelData) {
						System.out.println("Channel received: [" + chId + "] " + (size == 0 ? "EOF" : size + " bytes"));
					}
					Channel ch;
					synchronized(channels) {
						ch = channels.get(chId);
					}
					if (ch == null) {
						System.out.println("WARN: Channel " + chId + " do not exists");
					}
					else if (ch.direction == Direction.OUTBOUND) {
						System.out.println("WARN: Inbound packet to outbound channel " + chId);
					}
					else {
						try {
							if (size == 0) {
								if (traceChannelClose) {
									System.out.println("Channel closed: [" + ch.channelId + "] " + ch.direction);
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
				e.printStackTrace();
				System.out.println("Inbound mux stopped");
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
