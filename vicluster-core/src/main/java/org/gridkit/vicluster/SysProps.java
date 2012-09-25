package org.gridkit.vicluster;

import java.util.Collection;
import java.util.Map;

public interface SysProps {

	public String get(String propName);

	public Map<String, String> getAll(Collection<String> props);
	
	public void put(String propName, String prop);

	public void putAll(Map<String, String> props);
	
	public Map<String, String> toMap();
	
}
