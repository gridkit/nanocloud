package org.gridkit.nanocloud.interceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;

public class ThrowStub implements Interceptor, Serializable {

	private static final long serialVersionUID = 20130621L;

	private Throwable error;

	public ThrowStub(Throwable error) {
		this.error = error;
	}

	@Override
	public void handle(Interception call) {
		Throwable e = clone(error);
		call.setError(e);
	}

	private Throwable clone(Throwable e) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(e);
			oos.flush();
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bis);
			e = (Throwable) ois.readObject();
		} catch (Exception x) {
			// ignore
		}
		e.setStackTrace(new Exception().getStackTrace());
		return e;
	}
}
