package org.gridkit.vicluster.spi;

import java.util.List;
import java.util.Map;

public interface ViCloudContext {

	public <V> V getNamedInstance(String name, Class<V> type);
	
	public AttrBag getResource(String id);

	public AttrBag getResource(String type, String name);

	public List<AttrBag> selectResources(Selector selector);

	public AttrBag ensureResource(Iterable<Map.Entry<String, Object>> defaultValue);

	public AttrBag ensureResource(Selector s, Iterable<Map.Entry<String, Object>> defaultValue);

}
