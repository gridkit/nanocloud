package org.gridkit.vicluster.spi;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

public abstract class WriteThroughMap<K, V> extends AbstractMap<K, V> {

	private final Map<K, V> backingMap;
	private final Set<Map.Entry<K, V>> backingEntrySet;
	
	public WriteThroughMap(Map<K, V> backingMap) {
		this.backingMap = backingMap;
		this.backingEntrySet = new WriteThroughSet<Map.Entry<K,V>>(this.backingMap.entrySet()) {

			@Override
			protected boolean onInsert(java.util.Map.Entry<K, V> key) {
				throw new UnsupportedOperationException();
			}

			@Override
			protected boolean onRemove(java.util.Map.Entry<K, V> key) {
				return WriteThroughMap.this.onRemove(key.getKey());
			}
		};
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return backingEntrySet;
	}
	
	@Override
	public V get(Object key) {
		return backingMap.get(key);
	}

	@Override
	public V put(K key, V value) {
		V old = get(key);
		if (onPut(key, value)) {
			return super.put(key, value);
		}
		return old;
	}

	protected abstract boolean onPut(K key, V value);

	protected abstract boolean onRemove(K key);
}
