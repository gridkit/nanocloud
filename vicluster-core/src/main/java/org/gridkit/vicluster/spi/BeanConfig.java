package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO concurrency
class BeanConfig implements AttrBag {
	
	private final CloudContext cloudContext;
	private final List<Named<Object>> history = new ArrayList<Named<Object>>();
	private final Map<String, Object> scalarProps = new HashMap<String, Object>();
	private final Map<String, List<Object>> listProps = new HashMap<String, List<Object>>();
	
	boolean frozen = false;	
	private Set<String> underConstruction = new HashSet<String>();
	
	public BeanConfig(CloudContext cloudContext) {
		this.cloudContext = cloudContext;
	}

	void addEntry(String name, Object v) {
		history.add(Named.make(name, v));
		if (listProps.containsKey(name)) {
			listProps.get(name).add(v);
		}
		else if (scalarProps.containsKey(name)) {
			Object v0 = scalarProps.remove(name);
			List<Object> list = new ArrayList<Object>();
			list.add(v0);
			list.add(v);
			listProps.put(name, list);
		}
		else {
			scalarProps.put(name, v);
		}
	}
	
	public <V> V getInstance(Class<V> type) {
		return type.cast(getLast(INSTANCE));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <V> V getLast(String name) {
		synchronized (cloudContext) {
			cloudContext.enter();
			try {
				if (scalarProps.containsKey(name)) {
					Object v = scalarProps.get(name);
					v = ensureInstantiated(name, v);
					return (V)v;
				}
				else if (listProps.containsKey(name)){
					Object v = listProps.get(name).get(listProps.size() - 1);
					v = ensureInstantiated(name, v);
					return (V)v;
				}
				else {
					return null;
				}
			}
			finally {
				cloudContext.leave();
			}
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <V> List<V> getAll(String name) {
		synchronized (cloudContext) {
			cloudContext.enter();
			try {
				if (scalarProps.containsKey(name)) {
					V v = getLast(name);
					return Collections.singletonList(v);
				}
				else if (listProps.containsKey(name)) {
					List<Object> list = listProps.get(name);
					Object[] result = new Object[list.size()];
					for(int i = result.length - 1; i >= 0; --i) {
						result[i] = ensureInstantiated(name, list.get(i));
					}
					return (List)Arrays.asList(result);
				}
				else {
					return Collections.emptyList();
				}
			}
			finally {
				cloudContext.leave();
			}
		}
	}

	private Object ensureInstantiated(String name, Object v) {
		if (v instanceof SpiFactory) {
			if (underConstruction.contains(name)) {
				throw new RuntimeException("Recursive dependency detected, while intsntiating " + name + ", config " + this);
			}
			underConstruction.add(name);
			try {
				SpiFactory f = (SpiFactory) v;
				Object v1 = f.instantiate(cloudContext, name, this);
				cloudContext.addToInitQueue(this, v1);
				replace(name, v, v1);
				return v1;
			}
			finally {
				underConstruction.remove(name);
			}
		}
		else {
			return v;
		}
	}

	private void replace(String name, Object oldV, Object newV) {
		for(int i = 0; i != history.size(); ++i) {
			Named<Object> entry = history.get(i);
			if (entry.getKey().equals(name) && entry.getValue() == oldV) {
				history.set(i, Named.make(name, newV));
				break;
			}
		}
		if (scalarProps.containsKey(name)) {
			scalarProps.put(name, newV);
		}
		else {
			List<Object> list = listProps.get(name);
			for(int i = list.size() - 1; i >= 0; --i) {
				if (list.get(i) == oldV) {
					list.set(i, newV);
					break;
				}
			}			
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Named<Object> n: history) {
			if (sb.length() > 0) {
				sb.append('|');
			}
			if (n.getValue() instanceof SpiFactory) {
				sb.append(n.getKey()).append("=<lazy|" + n.getValue().getClass().getSimpleName() + ">");
			}
			else {
				sb.append(n.getKey()).append("=").append(n.getValue());
			}
		}
		return sb.toString();
	}
}
