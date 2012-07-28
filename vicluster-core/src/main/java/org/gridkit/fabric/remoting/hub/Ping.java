package org.gridkit.fabric.remoting.hub;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class Ping implements Callable<Void>, Serializable {

	private static final long serialVersionUID = 20120318L;

	@Override
	public Void call() throws Exception {
		return null;
	}
}
