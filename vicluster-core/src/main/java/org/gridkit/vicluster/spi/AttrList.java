package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AttrList implements Iterable<Map.Entry<String, Object>> {
	
	private List<String> names = new ArrayList<String>();
	private List<Object> values = new ArrayList<Object>();
	
	public void add(String name, Object value) {
		names.add(name);
		values.add(value);
	}

	public void addAll(Iterable<Entry<String, Object>> source) {
		for(Entry<String, Object> e: source) {
			add(e.getKey(), e.getValue());
		}
	}
	
	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return new Iterator<Entry<String, Object>>() {
			int n = 0;

			@Override
			public boolean hasNext() {
				return n < names.size();
			}

			@Override
			public Entry<String, Object> next() {
				Entry<String, Object> e = Named.make(names.get(n), values.get(n));
				++n;
				return e;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
