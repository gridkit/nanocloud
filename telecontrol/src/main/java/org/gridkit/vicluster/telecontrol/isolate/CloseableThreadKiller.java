package org.gridkit.vicluster.telecontrol.isolate;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.ThreadKiller;

public class CloseableThreadKiller implements ThreadKiller, Serializable {

	private static final long serialVersionUID = 20121101L;

	@Override
	public boolean tryToKill(Isolate isolate, Thread thread) {
		if (thread instanceof Closeable) {
			try {
				((Closeable)thread).close();
			}
			catch(IOException e) {
				// ignore;
			}
			return true;
		}
		else {
			return false;
		}
	}
}
