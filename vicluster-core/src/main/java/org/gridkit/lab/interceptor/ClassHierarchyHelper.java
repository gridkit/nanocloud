package org.gridkit.lab.interceptor;

public interface ClassHierarchyHelper {

	public void isParent(String childName, String parentName);

	public void isImplementor(String implementorName, String interfaceName);
	
}
