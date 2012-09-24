package org.gridkit.vicluster;

import java.util.List;

public interface ViCloud2<V extends ViNode> {

	public V byName(String pattern);

	public V byLabel(String pattern);
	
	public List<V> listByName(String pattern);

	public List<V> listByLabel(String pattern);
	
	public void shutdown();
	
}
