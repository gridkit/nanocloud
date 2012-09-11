package org.gridkit.zerormi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectRecoder {

	private RmiMarshaler marshalA;
	private RmiMarshaler marshalB;
	
	public ObjectRecoder(RmiMarshaler marshalA, RmiMarshaler marshalB) {
		this.marshalA = marshalA;
		this.marshalB = marshalB;
	}
	
	public Object recodeAB(Object obj) {
		try {
			return recode(obj, marshalA, marshalB);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Object recodeBA(Object obj) {
		try {
			return recode(obj, marshalB, marshalA);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object recode(Object obj, final RmiMarshaler out, final RmiMarshaler in) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oss = new ObjectOutputStream(bos) {
			{enableReplaceObject(true);}

			@Override
			protected Object replaceObject(Object obj) throws IOException {
				return out.writeReplace(obj);
			}
		};
		oss.writeObject(obj);
		oss.flush();
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bis) {
			{enableResolveObject(true);}

			@Override
			protected Object resolveObject(Object obj) throws IOException {
				return in.readResolve(obj);
			}
		};
		return ois.readObject();
	}
}
