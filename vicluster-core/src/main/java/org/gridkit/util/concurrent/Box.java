package org.gridkit.util.concurrent;

public interface Box<V> {

	public void setData(V data);

	public void setError(Exception e);
	
}
