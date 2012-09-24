package org.gridkit.vicluster.spi;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public abstract class WriteThroughSet<V> extends AbstractSet<V> {

	private Collection<V> backingSet;
	
	public WriteThroughSet(Collection<V> backingSet) {
		this.backingSet = backingSet;
	}

	@Override
	public boolean add(V e) {
		if (backingSet.contains(e)) {
			return false;
		}
		else {
			if (onInsert(e)) {
				backingSet.add(e);
			}
			return true;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		if (!backingSet.contains(o)) {
			return false;
		}
		else {
			if (onRemove((V)o)) {
				backingSet.remove(o);
			}
			return true;
		}
	}

	@Override
	public Iterator<V> iterator() {
		return new Iterator<V>() {
			
			Iterator<V> bi = backingSet.iterator();
			V lastKey;

			public boolean hasNext() {
				return bi.hasNext();
			}

			public V next() {
				return lastKey = bi.next();
			}

			public void remove() {
				if (onRemove(lastKey)) {
					bi.remove();
				}
			}
		};
	}

	@Override
	public int size() {
		return backingSet.size();		
	}
	
	protected abstract boolean onInsert(V key);

	protected abstract boolean onRemove(V key);
}
