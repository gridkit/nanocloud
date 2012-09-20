package org.gridkit.vicluster.spi;

import java.util.Map;

class Named<V> implements Map.Entry<String, V> {

	public static <V> Named<V> make(String name, V object) {
		return new Named<V>(name, object);
	}
	
	private final String name;
	private final V value;

	public Named(String name, V value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getKey() {
		return name;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}
}
