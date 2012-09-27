package org.gridkit.vicluster;

import java.util.List;

import org.gridkit.vicluster.spi.CloudConfigSet;

public interface ViCloud<V extends ViNode> {

	public V node(String pattern);
	
	public V byName(String pattern);

	public V byLabel(String pattern);
	
	public List<V> listByName(String pattern);

	public List<V> listByLabel(String pattern);
	
	public void applyConfig(CloudConfigSet rules);

	public void shutdown();
	
}
