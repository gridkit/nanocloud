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
package org.gridkit.util.vicontrol.isolate.coherence;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.DatagramSocket;

import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.ThreadKiller;

/**
 * This is heuristic thread killer which knows how to kill Coherence threads blocked on IO.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
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
