package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BeanConfig implements AttrBag {
	
	private final CloudContext cloudContext;
	private final List<Named<Object>> history = new ArrayList<Named<Object>>();
	private final Map<String, Object> scalarProps = new HashMap<String, Object>();
	private final Map<String, List<Object>> listProps = new HashMap<String, List<Object>>();
	
	boolean frozen = false;	
	
	public BeanConfig(CloudContext cloudContext) {
		this.cloudContext = cloudContext;
	}

	void addEntry(String name, Object v) {
		if (frozen) {
			throw new UnsupportedOperationException();
		}
		synchronized(cloudContext) {
			if (v instanceof SpiFactory) {
				Box b = new Box();
				b.factory = (SpiFactory) v;
				b.attrName = name;
				v = b;
			}
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
	}
	
	public <V> V getInstance(Class<V> type) {
		return type.cast(getLast(INSTANCE));
	}
	
	@Override
	public <V> V getLast(String name) {
		return convertOut(getLastInternal(name));
	}
	
	@Override
	public <V> List<V> getAll(String name) {
		return convertOut(this.<V>getAllInternal(name));
	}

	private <V> V getLastInternal(String name) {
		synchronized (cloudContext) {
			cloudContext.enter();
			try {
				if (scalarProps.containsKey(name)) {
					Object v = scalarProps.get(name);
					v = ensureInstantiated(name, v);
					return Any.cast(v);
				}
				else if (listProps.containsKey(name)){
					Object v = listProps.get(name).get(listProps.size() - 1);
					v = ensureInstantiated(name, v);
					return Any.cast(v);
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

	private <V> List<V> getAllInternal(String name) {
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
					return Any.cast(Arrays.asList(result));
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
		if (v instanceof Box) {
			Box b = (Box) v;
			if (b.object != null) {
				replace(name, b, b.object);
				return b.object;
			}
		}
		return v;
	}

	private <V> V convertOut(Object v) {
		if (v instanceof Box) {
			Box b = (Box)v;
			if (b.object == null) {
				synchronized(b) {
					if (b.object == null) {
						if (b.constructing) {
							throw new IllegalArgumentException("Cyclic dependency initializing " + b.attrName + " at " + this);
						}
						else {
							b.constructing = true;
						}
						Object val = b.factory.instantiate(cloudContext, b.attrName, this);
						cloudContext.init(this, val);
						b.object = val;
					}
				}
			}
			return Any.cast(b.object);
		}
		else {
			return Any.cast(v);
		}
	}
	
	private <V> List<V> convertOut(List<V> list) {
		for(int i = 0; i != list.size(); ++i) {
			if (list.get(i) instanceof Box) {
				list.set(i, this.<V>convertOut(list.get(i)));
			}
		}
		return list;
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
	
	private static class Box {
		
		private String attrName;
		private SpiFactory factory;

		private volatile boolean constructing;
		private volatile Object object;
		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Named<Object> n: history) {
			if (sb.length() > 0) {
				sb.append('|');
			}
			Object v = n.getValue(); 
			if (v instanceof Box) {
				Box b = (Box) v;
				if (b.object != null) {
					sb.append(n.getKey()).append("=").append(n.getValue());
				}
				else {
					sb.append(n.getKey()).append("=<lazy|" + b.factory.getClass().getSimpleName() + ">");
				}
			}
			else {
				sb.append(n.getKey()).append("=").append(n.getValue());
			}
		}
		return sb.toString();
	}
}
