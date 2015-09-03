package org.gridkit.zerormi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class ObjectOrException<T> implements Serializable {
    private static final long serialVersionUID = 42L;

    private T object;
    private Throwable exceptionWhileReadObject;

    public ObjectOrException(T object) {
        this.object = object;
    }

    public T getObject() throws Throwable {
        if (exceptionWhileReadObject != null) {
            throw exceptionWhileReadObject;
        } else {
            return object;
        }
    }

    private void writeObject(java.io.ObjectOutputStream outputStream) throws IOException {
        RmiGateway.RmiObjectOutputStream out = (RmiGateway.RmiObjectOutputStream) outputStream;
        out.writeObject(serializeToBytes(object, out));
    }

    private void readObject(java.io.ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        RmiGateway.RmiObjectInputStream in = (RmiGateway.RmiObjectInputStream) inputStream;
        try {
            object = deserializeFromBytes((byte[]) in.readObject(), in);
        } catch (Throwable t) {
            exceptionWhileReadObject = t;
        }
    }

    private byte[] serializeToBytes(T object, RmiGateway.RmiObjectOutputStream prototype) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final RmiGateway.RmiObjectOutputStream out =
                new RmiGateway.RmiObjectOutputStream(prototype.getName(), prototype.getChannel(), byteArrayOutputStream);
        out.writeObject(object);
        out.flush();
        return byteArrayOutputStream.toByteArray();
    }


    @SuppressWarnings("unchecked")
    private T deserializeFromBytes(byte[] data, RmiGateway.RmiObjectInputStream prototype) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        final RmiGateway.RmiObjectInputStream in =
                new RmiGateway.RmiObjectInputStream(prototype.getName(), prototype.getChannel(), byteArrayInputStream);
        return (T) in.readObject();
    }
}
