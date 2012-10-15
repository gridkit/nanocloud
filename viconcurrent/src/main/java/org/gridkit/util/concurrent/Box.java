package org.gridkit.util.concurrent;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Box<V> {

	public void setData(V data);

	public void setError(Throwable e);
	
}
