package org.gridkit.zerormi;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
interface Box<V> {

	public void setData(V data);

	public void setError(Exception e);
	
}
