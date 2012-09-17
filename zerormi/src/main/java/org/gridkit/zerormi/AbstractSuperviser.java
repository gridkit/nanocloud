package org.gridkit.zerormi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gridkit.zerormi.ByteStream.Duplex;
import org.slf4j.Logger;

public abstract class AbstractSuperviser implements Superviser {
	
	protected final String name;
	protected final List<Object> components = new ArrayList<Object>();
	protected boolean terminated;
	
	public AbstractSuperviser(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public synchronized void addComponent(Object component) {
		if (terminated) {
			throw new IllegalStateException("Composition [" + name + "] is terminated");
		}
		components.add(component);
	}
	
	@Override
	public void onWarning(SuperviserEvent event) {
		logWarn(event);
	}

	@Override
	public void onTermination(SuperviserEvent event) {
		synchronized(this) {
			logInfo(event);
			if (terminated) {
				return;
			}
			else {
				terminated = true;
			}
		}			
		stopAll();
	}

	protected void terminate() {
		synchronized(this) {
			if (terminated) {
				return;
			}
			else {
				terminated = true;
			}
		}
		stopAll();
	}
	
	@Override
	public void onFatalError(SuperviserEvent event) {
		synchronized(this) {
			logError(event);
			dumpStatus();
			if (terminated) {
				return;
			}
			else {
				terminated = true;
			}
		}
		stopAll();
	}

	protected abstract Logger getLogger();

	protected abstract Logger getLogger(Object component);
		
	protected void dumpStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("[" + name + "] - composition details");
		for(Object obj: components) {
			sb.append('\n');
			if (obj instanceof Component) {
				sb.append(obj.getClass().getSimpleName());
				sb.append(": ");
				sb.append(safeToString(obj));
				sb.append(" -> ");
				sb.append(safeStatus(obj));
			}
			else {
				sb.append(obj.getClass().getSimpleName());
				sb.append(": ");
				sb.append(safeToString(obj));
				sb.append(" -> ");				
				sb.append(safeStatus(obj));
			}
		}
		getLogger().info(sb.toString());
	}
	
	protected Object safeStatus(Object obj) {
		if (obj instanceof Component) {
			try {
				return ((Component)obj).getStatusLine();
			}
			catch(Exception e) {
				return "getStatusLine() failed " + e.toString();
			}
		}
		else {
			return "N/A";
		}
	}

	private Object safeToString(Object obj) {
		try {
			return obj.toString();
		}
		catch(Exception e) {
			return "<error>";
		}
	}

	protected void logInfo(SuperviserEvent event) {
		getLogger(event.getComponent()).info(event.toString());
	}
	
	protected void logWarn(SuperviserEvent event) {
		getLogger(event.getComponent()).warn(event.toString());
	}

	protected void logError(SuperviserEvent event) {
		getLogger(event.getComponent()).error(event.toString());
	}

	protected void stopAll() {
		while(true) {
			Object component;
			synchronized(this) {
				if (components.isEmpty()) {
					return;
				}
				else {
					Iterator<Object> it = components.iterator();
					component = it.next();
					it.remove();
				}
			}
			stop(component);
		}
	}

	protected boolean isDiscontinued(Duplex stream) {
		try {
			return !stream.isConnected();
		}
		catch(Exception e) {
			return false;
		}
	}
	
	protected void stop(Object obj) {
		if (obj instanceof RmiChannel2) {
			((RmiChannel2)obj).destroy();
		}
		else if (obj instanceof DuplexObjectPipe) {
			((DuplexObjectPipe)obj).close();
		}
		else if (obj instanceof DuplexBlobPipe) {
			((DuplexBlobPipe)obj).close();
		}
		else if (obj instanceof ByteStream.Duplex) {
			((ByteStream.Duplex)obj).getOutput().endOfStream();
		}
		else {
			throw new RuntimeException("Cannot stop " + obj);
		}
	}	
}
