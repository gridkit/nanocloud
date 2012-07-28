package org.gridkit.util.vicontrol.isolate.coherence;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.DatagramSocket;

import org.gridkit.util.vicontrol.isolate.Isolate;
import org.gridkit.util.vicontrol.isolate.ThreadKiller;

@SuppressWarnings("serial")
public class ShutdownSocketDestructor implements ThreadKiller, Runnable, Serializable {

	@Override
	public void run() {
		Isolate.currentIsolate().addThreadKiller(this);
	}

	@Override
	public boolean tryToKill(Isolate isolate, Thread t) {
		Object target = getField(t, "target");
		if (target == null) {
			return false;
		}
		String cn = target.getClass().getName();
		if (cn.startsWith("com.tangosol.coherence.component")
				&& cn.contains("PacketListener")) {
			try {
				Object udpSocket = getField(target, "__m_UdpSocket");
				DatagramSocket ds = (DatagramSocket) getField(udpSocket, "__m_DatagramSocket");
				ds.close();
				isolate.getStdErr().println("Closing socket for " + t.getName());
				return true;
			}
			catch(Exception e) {
				// ignore
			}
		}
		else if (cn.startsWith("com.tangosol.coherence.component")
					&& cn.contains("PacketPublisher")) {
			try {
				Object udpSocket = getField(target, "__m_UdpSocketUnicast");
				DatagramSocket ds = (DatagramSocket) getField(udpSocket, "__m_DatagramSocket");
				ds.close();
				isolate.getStdErr().println("Closing socket for " + t.getName());
			}
			catch(Exception e) {
				// ignore;
			}
			try {
				Object udpSocket = getField(target, "__m_UdpSocketMulticast");
				DatagramSocket ds = (DatagramSocket) getField(udpSocket, "__m_DatagramSocket");
				ds.close();
				isolate.getStdErr().println("Closing socket for " + t.getName());
			}
			catch(Exception e) {
				// ignore;
			}
		}
		return false;
	}
	
	public static Object getField(Object x, String field) {
		try {
			Field f = null;
			Class<?> c = x.getClass();
			while(f == null && c != Object.class) {
				try {
					f = c.getDeclaredField(field);
				} catch (NoSuchFieldException e) {
				}
				if (f == null) {
					c = c.getSuperclass();
				}
			}
			if (f != null) {
				f.setAccessible(true);
				return f.get(x);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		throw new IllegalArgumentException("Cannot get '" + field + "' from " + x.getClass().getName());
	}	
}
