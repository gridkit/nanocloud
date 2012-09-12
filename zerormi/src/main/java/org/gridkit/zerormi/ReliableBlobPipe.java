package org.gridkit.zerormi;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.TreeMap;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.ComponentSuperviser.SuperviserEvent;

public class ReliableBlobPipe implements DuplexBlobPipe {

	static boolean TRACE = false;
	private static int MAX_PACKET_SIZE = 64 << 20;
	
	private static final byte MDATA = 0;
	private static final byte MACK1 = 1;
	private static final byte MACK2 = 2;
	private static final byte MNACK = 3;
	// ACK for ACK2 or NACK
	private static final byte MACKNACK = 4;
	// Used as handshake indicator if no ACKs to send
	private static final byte MSYNC = 5;
	
	private final String name;
	private final PipeSuperviser superviser;
	private BlobReceiver receiver;
	private volatile boolean terminated = false; 
	
	private ByteStream.Duplex activeStream;
	private InputHandler inputHander;
	private boolean resyncing;
	
	private int nextOutgoingMsgId = 0;	
	@SuppressWarnings("unused")
	private int nextIncomingMsgId = 0;
	
	private TreeMap<Integer, Message> outQ = new TreeMap<Integer, Message>();
	private TreeMap<Integer, Message> inQ = new TreeMap<Integer, Message>();
	
	public ReliableBlobPipe(String name, PipeSuperviser superviser) {
		this.name = name;
		this.superviser = superviser;
	}
	
	
	@Override
	public synchronized void bind(BlobReceiver receiver) {
		if (receiver == null) {
			throw new NullPointerException("Receiver should not be null");
		}
		if (this.receiver == null) {
			this.receiver = receiver;
		}
		else {
			throw new IllegalStateException("Pipe " + this + " is already bound");
		}
	}

	public synchronized void setStream(ByteStream.Duplex stream) {
		if (terminated) {
			throw new IllegalStateException("Terminated");
		}
		if (receiver == null) {
			throw new IllegalStateException("Bind receiver first");
		}
		disconnectStream();
		if (stream != null) {
			activeStream = stream;
			inputHander = new InputHandler(activeStream);
			resync();
		}
	}
	
	@Override
	public synchronized FutureEx<Void> sendBinary(byte[] bytes) {
		if (terminated) {
			return FutureBox.errorFuture(new EOFException("Closed"));
		}
		Message message = new Message();
		message.id = nextId();
		message.body = bytes;
		message.ack = new FutureBox<Void>();
		addToOutboundQueue(message);
		sendMessage(message);
		return message.ack;
	}

	@Override
	public synchronized void close() {
		terminated = true;
		disconnectStream();
		for(Message msg: outQ.values()) {
			try {
				msg.ack.setError(new EOFException("Closed"));
			}
			catch(Exception e) {
				// ignore;
			}
		}
		superviser.onTermination(SuperviserEvent.newClosedEvent(this));				
	}

	// test only
	int getInboundQueueSize() {
		return inQ.size();
	}
	
	// test only
	int getOutboundQueueSize() {
		return outQ.size();
	}
	
	private boolean verify(String reason, boolean condition) {
		if (!condition) {
			superviser.onFatalError(SuperviserEvent.newUnexpectedError(this, reason));
		}
		return condition;
	}

	private synchronized void disconnectStream() {
		if (activeStream != null) {
			inputHander.dead = true;
			activeStream = null;
			inputHander = null;
		}		
	}

	private synchronized void resync() {
		if (online()) {
			if (!inQ.isEmpty()) {
				// TODO retransmit also, recent ACKNACKs
				byte[] rsync = initPacket(4 * inQ.size());

				if (TRACE) {
					StringBuilder sb = new StringBuilder();
					for(Message msg: inQ.values()) {
						if (sb.length() > 0) {
							sb.append(", ");
						}
						if (msg.acked) {
							sb.append("ack2 " +msg.id);
						}
						else {
							sb.append("ack1 " +msg.id);
						}
					}
					
					System.out.println(name + ": sendResync-> " + sb);
				}

				ByteBuffer bb = ByteBuffer.wrap(rsync);
				bb.getInt();
				for(Message msg: inQ.values()) {
					int ack;
					if (msg.acked) {
						ack = MACK2;
					}
					else {
						ack = MACK1;
					}
					bb.putInt(ackQuard(ack, msg.id));
				}
				sendPacket(rsync);
				resyncing = true;
			}
			else {
				sendSync();
				resyncing = true;
			}
		}		
	}
	
	private synchronized void retransmit() {
		for(Message msg: outQ.values()) {
			if (!msg.acked) {
				sendMessage(msg);
			}			
		}
		for(Message msg: inQ.values()) {
			if (msg.error != null) {
				sendNack(msg);
			}
		}		
	}

	void messageReceived(byte[] bytes) {
		byte mtype = bytes[0];
		switch(mtype) {
		case MDATA:
			processMessage(bytes);
			break;
		case MACK1:
		case MACK2:
		case MACKNACK:
			processAcks(bytes);
			break;
		case MNACK:
			processNack(bytes);
			break;
		case MSYNC:
			break;
		default:
			rejectStreamAsCorrupted("Unknown message type: " + mtype);
		}
		
		synchronized(this) {
			if (resyncing) {
				retransmit();
				resyncing = false;
			}
		}
	}

	private synchronized void rejectStreamAsCorrupted(String reason) {
		rejectStream(new IOException("Corrupt data stream: " + reason));		
	}

	private synchronized void rejectStream(Exception error) {
		if (activeStream != null) {
			ByteStream.Duplex oldStream = activeStream;
			activeStream = null;
			superviser.onStreamRejected(this, oldStream, error);
		}
	}

	private synchronized void processMessage(byte[] bytes) {
		int id = readId(bytes, 0);
		if (inQ.containsKey(id)) {
			// prevent duplicate processing
			Message msg = inQ.get(id);
			if (msg.acked) {
				sendAck(MACK2, msg);
			}
			else {
				sendAck(MACK1, msg);				
			}
		}
		else {
			Message msg = new Message();
			msg.id = id;
			msg.body = Arrays.copyOfRange(bytes, 4, bytes.length);
			if (TRACE) {
				System.out.println(name + ": receiveMsg " + msg.id + " " + Arrays.toString(msg.body));
			}
			addToInboundQueue(msg);
			sendAck(MACK1, msg);
			DeliveryWatch watch = new DeliveryWatch(msg);
			FutureEx<Void>  ack = receiver.blobReceived(msg.body);
			ack.addListener(watch);
		}
	}

	private void processAcks(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		while(buf.remaining() > 0) {
			int q = buf.getInt();
			int ack = ackTypeFromQuard(q);
			int mid = msgIdFromQuard(q);
			ack(ack, mid);
		}
	}

	private void processNack(byte[] bytes) {
		int id = readId(bytes, 0);
		try {
			String text = readUtf(bytes, 4, bytes.length - 4);
			notifyError(id, text);
		}
		catch(IOException e) {
			notifyError(id, "Cannot parse error message: " + e.toString());
		}
		sendAckNack(id);
	}

	private synchronized void sendMessage(Message msg) {
		if (online()) {
			byte[] packet = newPacketMessage(msg.id, msg.body);
			if (TRACE) {
				System.out.println(name + ": sendMsg " + msg.id + " " + Arrays.toString(msg.body));
			}
			sendPacket(packet);
		}
	}

	private synchronized void sendAck(int ack, Message msg) {
		if (online()) {
			byte[] packet = newPacketAck(ack, msg.id);
			if (TRACE) {
				System.out.println(name + ": sendAck" + ack + " " + msg.id);
			}
			sendPacket(packet);
		}		
	}

	private synchronized void sendAckNack(int msgId) {
		if (online()) {
			byte[] packet = newPacketAck(MACKNACK, msgId);
			if (TRACE) {
				System.out.println(name + ": sendAckNack " + msgId);
			}
			sendPacket(packet);
		}		
	}
	
	private synchronized void sendNack(Message msg) {
		if (online()) {
			byte[] packet = newPacketNack(msg.id, msg.error);
			if (TRACE) {
				System.out.println(name + ": sendNack " + msg.id + " " + msg.error);
			}
			sendPacket(packet);
		}				
	}

	private synchronized void sendSync() {
		if (online()) {
			byte[] packet = newPacketSync();
			if (TRACE) {
				System.out.println(name + ": sendSync");
			}
			sendPacket(packet);
		}				
	}
	
	private void sendPacket(byte[] packet) {
		try {
			if (TRACE) {
				System.out.println(name + ": wire-> " + packet.length + " bytes, " + Arrays.toString(packet));
			}
			activeStream.getOutput().handle(ByteBuffer.wrap(packet));
		}
		catch(Exception e) {
			rejectStream(e);
		}
	}

	private byte[] initPacket(int size) {
		int fsize = (4 + size + 3) & (~3);
		byte[] pack = new byte[fsize];
		pack[0] = (byte)(size >> 24);
		pack[1] = (byte)(size >> 16);
		pack[2] = (byte)(size >> 8);
		pack[3] = (byte)size;
		return pack;
		
	}

	private byte[] newPacketMessage(int id, byte[] body) {
		byte[] pack = initPacket(4 + body.length);
		pack[4] = MDATA;
		pack[5] = (byte)(id >> 16);
		pack[6] = (byte)(id >> 8);
		pack[7] = (byte)(id);
	
		System.arraycopy(body, 0, pack, 8, body.length);
		
		return pack;
	}


	private byte[] newPacketAck(int ack, int id) {
		byte[] pack = initPacket(4);
		pack[4] = (byte) ack;
		pack[5] = (byte)(id >> 16);
		pack[6] = (byte)(id >> 8);
		pack[7] = (byte)(id);
	
		return pack;
	}


	private byte[] newPacketNack(int id, String error) {
		byte[] msg = toUTF8(error);		
		byte[] pack = initPacket(4 + msg.length);
		pack[4] = MNACK;
		pack[5] = (byte)(id >> 16);
		pack[6] = (byte)(id >> 8);
		pack[7] = (byte)(id);

		System.arraycopy(msg, 0, pack, 8, msg.length);
		
		return pack;
	}

	private byte[] newPacketSync() {
		byte[] pack = initPacket(4);
		pack[4] = (byte) MSYNC;
	
		return pack;
	}

	private byte[] toUTF8(String text) {
		try {
			return text.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			throw new Error("Impossible");
		}
	}


	private synchronized boolean online() {
		return activeStream != null;
	}

	private synchronized void addToInboundQueue(Message msg) {
		inQ.put(msg.id, msg);
	}

	private synchronized void addToOutboundQueue(Message message) {
		outQ.put(message.id, message);
	}


	private void ack(int type, int id) {
		if (type == MACKNACK) {
			acknack(id);
		}
		else {
			ackmsg(type, id);
		}
	}
	
	private synchronized void acknack(int id) {
		if (TRACE) {
			System.out.println(name + ": receiveAckNack" + id);
		}
		removeInbound(id);
	}
	
	private synchronized void removeInbound(int id) {
		inQ.remove(id);
	}

	private synchronized void ackmsg(int type, int id) {
		if (TRACE) {
			System.out.println(name + ": receiveAck" + type + " " + id);
		}
		Message msg = outQ.get(id);
		if (msg != null) {
			if (type == MACK1) {
				msg.acked = true;
				msg.body = null; // free buffer
			}
			else {
				// ACK2 - message fully acknowledged
				msg.ack.setData(null);
				outQ.remove(id);
				sendAckNack(id);
			}
		}
		else if (type == MACK2) {
			// We may receive duplicated MACK2
			sendAckNack(id);
		}
		// TODO warnings
	}

	private synchronized void notifyError(int id, String text) {
		if (TRACE) {
			System.out.println(name + ": receiveNack" + id + " " + text);
		}				
		Message msg = outQ.get(id);
		if(msg != null) {
			msg.ack.setError(new IOException("Remote error: " + text));
			outQ.remove(id);
		}
		// if message is not found, it is OK. NACK may be a duplicate.
	}
	
	private String readUtf(byte[] bytes, int offs, int len) throws IOException {
		return new String(bytes, offs, len, "UTF8");
	}

	private int ackQuard(int ack, int msgId) {
		return ((0xFF & ack) << 24) | msgId;
	}
	
	private int ackTypeFromQuard(int quard) {
		return 0xFF & (quard >> 24);
	}

	private int msgIdFromQuard(int quard) {
		return 0xFFFFFF & quard;
	}
	
	private int readId(byte[] bytes, int i) {
		int id = 0;
		// ignore highest byte
		id |= (0xFF & bytes[i + 1]) << 16;
		id |= (0xFF & bytes[i + 2]) << 8;
		id |= (0xFF & bytes[i + 3]);
		return id;
	}

	private int nextId() {
		int id = nextOutgoingMsgId;
		nextOutgoingMsgId++;
		nextOutgoingMsgId &= 0x00FFFFFF; // use 3 bytes only
		return id;
	}

	@Override
	public String toString() {
		return name;
	}

	private class InputHandler implements ByteStream.Sink {
		
		@SuppressWarnings("unused")
		private ByteStream.Duplex stream;
		
		// next message
		private byte[] nextMsgBuffer;
		private int nextOffset;
		private int remaining;
		private boolean dead;
		
		public InputHandler(ByteStream.Duplex stream) {
			this.stream = stream;
			stream.bindInput(this);
		}		
		
	
		@Override
		public void brokenStream(Exception error) {
			rejectStream(error);			
		}


		@Override
		public void endOfStream() {
			rejectStream(null);			
		}

		@Override
		public synchronized void handle(ByteBuffer data) {
			try {
				if (dead) {
					return;
				}
				while(data.remaining() > 0) {
					if (nextMsgBuffer != null) {
						fillNextMessage(data);
						if (isNextMessageReady()) {
							processNextMessage();
							continue;
						}
						else {
							if (!verify("Stream parser error", data.remaining() == 0)) {
								dead = true;
							}
							return;
						}
					}
					int size = data.getInt();
					if (size == 0) {
						dead = true;
						rejectStreamAsCorrupted("Empty packet encountered");
						return;						
					}
					if (size > MAX_PACKET_SIZE) {
						dead = true;
						rejectStreamAsCorrupted("Packet is too large " + size);
						return;
					}
					else {
						nextMsgBuffer = new byte[size];
						nextOffset = 0;
						// allign at 4 bytes
						remaining = (size + 3) & (~3);
					}
				}
			}
			catch(Exception e) {
				rejectStream(e);
			}
		}
	
		private void processNextMessage() {
			byte[] data = nextMsgBuffer;
			nextMsgBuffer = null;
			nextOffset = 0;
			remaining = 0;
			
			messageReceived(data);			
		}

		private boolean isNextMessageReady() {
			return remaining == 0;
		}
	
		private synchronized void fillNextMessage(ByteBuffer data) {
			int rlen = nextMsgBuffer.length - nextOffset;
			if (data.remaining() < rlen) {
				rlen = data.remaining();
			}
			data.get(nextMsgBuffer, nextOffset, rlen);
			nextOffset += rlen;
			remaining -= rlen;
			if (nextMsgBuffer.length == nextOffset) {
				while(remaining > 0 && data.remaining() > 0) {
					data.get();
					--remaining;
				}
			}
		}
	}

	public interface PipeSuperviser extends ComponentSuperviser {
		
		public void onStreamRejected(ReliableBlobPipe pipe, ByteStream.Duplex stream, Exception e);
		
	}
	
	private static class Message {
		int id;
		byte[] body;
		boolean acked;
		String error;
		FutureBox<Void> ack;
	}

	private class DeliveryWatch implements Box<Void> {

		private Message msg;
		
		public DeliveryWatch(Message msg) {
			this.msg = msg;
		}

		@Override
		public void setData(Void data) {
			msg.acked = true;
			sendAck(MACK2, msg);
		}

		@Override
		public void setError(Throwable e) {
			msg.error = e.toString();
			sendNack(msg);
		}		
	}
//	
//	private static class MessageComparator implements Comparator<Message> {
//		@Override
//		public int compare(Message o1, Message o2) {
//			return o1.id - o2.id;
//		}
//	}
}
