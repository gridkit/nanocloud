package org.gridkit.zerormi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;

public class SmartObjectSerializer implements DuplexObjectPipe, DuplexBlobPipe.BlobReceiver {
	
	static boolean TRACE = false;
	
	private final DuplexBlobPipe binaryPipe;
	private final RmiMarshaler marshaler;
	private final ClassProvider classProvider;
	private DuplexObjectPipe.ObjectReceiver receiver;	
	private volatile boolean terminated = false;
	
	public SmartObjectSerializer(DuplexBlobPipe pipe, RmiMarshaler marshaler, ClassProvider cp) {
		this.binaryPipe = pipe;
		this.marshaler = marshaler;
		this.classProvider = cp;
		pipe.bind(this);
	}

	private byte[] toBytes(Object obj) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos) {

			{
				enableReplaceObject(true);
			}

			@Override
			protected Object replaceObject(Object obj) throws IOException {
				Object replacement = marshaler.writeReplace(obj);
				return replacement;
			}
		};
		oos.writeObject(obj);
		oos.flush();
		return bos.toByteArray();
	}

	private Object fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bis) {

			{
				enableResolveObject(true);
			}
			
			@Override
			protected Class<?> resolveClass(ObjectStreamClass desc)	throws IOException, ClassNotFoundException {
				return classProvider.classForName(desc.getName());
			}

			@Override
			protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
				return classProvider.proxyClassForTypes(interfaces);
			}

			@Override
			protected Object resolveObject(Object obj) throws IOException {
				Object resolved = marshaler.readResolve(obj);
				return resolved;
			}			
		};
		
		return ois.readObject();
	}
	
	@Override
	public synchronized void bind(ObjectReceiver receiver) {
		if (this.receiver == null) {
			this.receiver = receiver;
		}
		else {
			throw new IllegalStateException("Pipe " + this + " is already bound");
		}
	}

	@Override
	public FutureEx<Void> sendObject(Object object) {
		if (terminated) {
			FutureBox<Void> fb = new FutureBox<Void>();
			fb.setError(newPipeClosedException());
			return fb;
		}
		else {
			FutureBox<Void> ack = new FutureBox<Void>();
			
			try {
				byte[] bytes = toBytes(object);
				if (TRACE) {
					System.out.println(binaryPipe + ": objectSend -> " + object);
				}
				FutureEx<Void> sf = binaryPipe.sendBinary(bytes);
				sf.addListener(ack);
			}
			catch(Exception ee) {
				ack.setError(ee);
			}
			
			return ack;
		}
	}

	private Exception newPipeClosedException() {
		// TODO use specific exceptions
		return new IOException("Pipe closed");
	}

	@Override
	public synchronized void close() {
		if (!terminated) {
			terminated = true;
			binaryPipe.close();
			if (receiver !=null) {
				receiver.closed();
			}
		}
	}
	
	@Override
	public FutureEx<Void> blobReceived(byte[] data) {
		Object obj;
		try {
			obj = fromBytes(data);
			if (TRACE) {
				System.out.println(binaryPipe + ": objReceived <- " +  obj);
			}
		} catch (Exception e) {
			FutureBox<Void> fb = new FutureBox<Void>();
			fb.setError(e);
			return fb;
		}
		// TODO queue
		FutureBox<Void> fb = new FutureBox<Void>();
		FutureEx<Void> ack = receiver.objectReceived(obj);
		ack.addListener(fb);
		return fb;
	}

	@Override
	public void closed() {
		// TODO
		// synchronization is not used to reduce chance potential deadlock
		// though it is still possible 
		if (!terminated) {
			close();
		}		
	}

	@Override
	public String toString() {
		return "java:" + binaryPipe;
	}
}
