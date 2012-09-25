package org.gridkit.vicluster.isolate;

import java.net.URL;

public interface Isolation {

	/**
	 * Adds package to list of isolated packages
	 */	
	public void includePackage(String packageName);

	/**
	 * Forces {@link Isolate} not to isolate class even if it is falling to {@link Isolate}'s scope.
	 * @param type
	 */
	public void excludeClass(String className);

	/**
	 * Forces {@link Isolate} not to isolate class even if it is falling to {@link Isolate}'s scope.
	 * @param type
	 */
	public void excludeClass(Class<?> type);
	
	public void addToClasspath(String path);

	public void addToClasspath(URL path);

	public void removeFromClasspath(String path);

	public void removeFromClasspath(URL path);
}
