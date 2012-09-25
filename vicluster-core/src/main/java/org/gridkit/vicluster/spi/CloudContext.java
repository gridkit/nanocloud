package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class CloudContext implements ViCloudContext {

	static boolean TRACE = true;
	
	private Collection<BeanConfig> finalConfig = new HashSet<BeanConfig>();
	
	private Map<String, Index> indexes = new HashMap<String, Index>();
	{
		indexes.put(AttrBag.ID, new Index());
		indexes.put(AttrBag.TYPE, new Index());
	}

	private List<ConfigRule> rules = new ArrayList<ConfigRule>();
	
	// TODO Not thread safe !!!
	private int entryCount;
	private List<DeferedInit> deferedQueue = new ArrayList<DeferedInit>();
	
	CloudContext() {
		
	}
	
	void enter() {
		++entryCount;
	}

	void leave() {
		--entryCount;		
		while (entryCount == 0 && !deferedQueue.isEmpty()) {
			++entryCount;
			try {
				DeferedInit d = deferedQueue.remove(deferedQueue.size() - 1);
				init(d.config, d.obj);
			}
			finally {
				--entryCount;
			}
		}		
	}

	void init(BeanConfig config, Object obj) {
		if (obj instanceof CloudContextAware) {
			((CloudContextAware)obj).setContext(this);
		}
		if (obj instanceof SpiConfigurable) {
			((SpiConfigurable)obj).configure(config);
		}		
	}

	void addToInitQueue(BeanConfig config, Object obj) {
		deferedQueue.add(new DeferedInit(config, obj));		
	}

	public synchronized void addRule(ConfigRule rule) {
		rules.add(0, rule);		
	}
	
	public synchronized <V> V getNamedInstance(String name, Class<V> type) {
		if (name == null) {
			throw new NullPointerException("name should not be null");
		}
		AttrBag bean = getResource(name, type.getName());
		Object obj = bean.getLast(AttrBag.INSTANCE);
		return type.cast(obj);
	}
	
	public synchronized <V> V ensureNamedInstance(String name, Class<V> type) {
		AttrList proto = new AttrList();
		proto.add(AttrBag.NAME, name);
		proto.add(AttrBag.TYPE, type.getName());
		
		ensureResource(Selectors.matchAll(proto), proto);
		
		return getNamedInstance(name, type);
	}
	
	@Override
	public synchronized AttrBag getResource(String id) {
		List<AttrBag> beans = selectResources(Selectors.id(id));
		if (beans.isEmpty()) {
			return null;
		}
		else if (beans.size() == 1){
			return beans.get(0);
		}
		else {
			throw new IllegalArgumentException("Id is not unique: " + id);
		}
	}

	@Override
	public synchronized AttrBag getResource(String name, String type) {
		List<AttrBag> beans = selectResources(Selectors.name(name, type));	
		if (beans.isEmpty()) {
			return null;
		}
		else if (beans.size() == 1){
			return beans.get(0);
		}
		else {
			throw new IllegalArgumentException("name/type is not unique: " + name + "/" + type);
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized List<AttrBag> selectResources(Selector s) {
		return (List)select(finalConfig, s);
	}

	@Override
	public synchronized AttrBag ensureResource(Iterable<Entry<String, Object>> prototype) {
		return ensureResource(Selectors.matchAll(prototype), prototype);
	}
	
	@Override
	public synchronized AttrBag ensureResource(Selector s, Iterable<Entry<String, Object>> prototype) {
		enter();
		try {
			List<AttrBag> beans = selectResources(s);
			if (beans.isEmpty()) {
				BeanConfig newbean = new BeanConfig(this);
				for(Entry<String, Object> entry: prototype) {
					newbean.addEntry(entry.getKey(), entry.getValue());
				}
				if (!s.match(newbean)) {
					throw new IllegalArgumentException("Selector does not match prototype");
				}
				fireRules(newbean, rules);
				finalConfig.add(newbean);
				return newbean;
			}
			else if (beans.size() == 1) {
				return beans.get(0);
			}
			else {
				throw new IllegalArgumentException("Multiple matches for selector: " + s);
			}
		}
		finally {
			leave();
		}
	}

	@Override
	public synchronized void destroyResources(Selector selector) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	private List<BeanConfig> select(Iterable<BeanConfig> beans, Selector s) {
		List<BeanConfig> result = new ArrayList<BeanConfig>();
		for(BeanConfig bean : beans) {
			if (s.match(bean)) {
				result.add(bean);
			}
		}
		return result;
	}
	
	private void fireRules(BeanConfig config, List<ConfigRule> rules) {
		Set<ConfigRule> doneList = new HashSet<ConfigRule>();
	
		if (TRACE) {
			System.out.println("Apply rules: " + config);
		}
		
		applyAllRules:
		while(true) {
			for(ConfigRule rule: rules) {
				if (doneList.contains(rule)) {
					continue;
				}
				if (rule.match(config)) {
					doneList.add(rule);
					rule.fire(config);
					System.out.println("  matched - " + config);
					continue applyAllRules;
				}
			}
			break;
		}
	}
	
	private static class Index {

		Map<Object, Set<BeanConfig>> inverted = new HashMap<Object, Set<BeanConfig>>();
		
	}
	
	private static class DeferedInit {
		
		BeanConfig config;
		Object obj;

		public DeferedInit(BeanConfig config, Object obj) {
			this.config = config;
			this.obj = obj;
		}
	}
}
