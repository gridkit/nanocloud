package org.gridkit.vicluster.spi;

import java.util.List;

public interface AttrBag {
	
	public final static String ID = "id";
	public final static String NAME = "name";
	public final static String TYPE = "type";
	public final static String LABEL = "label";
	public final static String INSTANCE = "instance";
	public final static String DESTRUCTOR = "destructor";
		
	public boolean containsKey(String name);
	
	public <V> V getLast(String name);

	public <V> List<V> getAll(String name);

	public <V> List<V> getAllInOrder(String name);

	public <V> V getInstance(Class<V> type);
}
