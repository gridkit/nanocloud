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

	private Collection<BeanConfig> finalConfig = new HashSet<BeanConfig>();
	
	private Map<String, Index> indexes = new HashMap<String, Index>();
	{
		indexes.put(AttrBag.ID, new Index());
		indexes.put(AttrBag.TYPE, new Index());
	}

	private List<ConfigRule> rules = new ArrayList<ConfigRule>();
	
	// TODO Not thread safe !!!
	private int entryCount;
	private List<Defered> deferedQueue = new ArrayList<Defered>();
	
	CloudContext() {
		
	}
	
	public void enter() {
		++entryCount;
	}

	public void leave() {
		--entryCount;		
		while (entryCount == 0 && !deferedQueue.isEmpty()) {
			++entryCount;
			try {
				Defered d = deferedQueue.remove(deferedQueue.size() - 1);
				init(d.config, d.obj);
			}
			finally {
				--entryCount;
			}
		}		
	}

	private void init(BeanConfig config, Object obj) {
		if (obj instanceof CloudContextAware) {
			((CloudContextAware)obj).setContext(this);
		}
		if (obj instanceof SpiConfigurable) {
			((SpiConfigurable)obj).configure(config);
		}		
	}

	void addToInitQueue(BeanConfig config, Object obj) {
		deferedQueue.add(new Defered(config, obj));		
	}

	public void addRule(ConfigRule rule) {
		rules.add(rule);		
	}
	
	public <V> V getNamedInstance(String name, Class<V> type) {
		Object obj = getResource(name, type.getName());
		return type.cast(obj);
	}
	
	@Override
	public AttrBag getResource(String id) {
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
	public AttrBag getResource(String name, String type) {
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
	public List<AttrBag> selectResources(Selector s) {
		return (List)select(finalConfig, s);
	}

	@Override
	public AttrBag ensureResource(Iterable<Entry<String, Object>> prototype) {
		return ensureResource(Selectors.matchAll(prototype), prototype);
	}
	
	@Override
	public AttrBag ensureResource(Selector s, Iterable<Entry<String, Object>> prototype) {
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
	
		applyAllRules:
		while(true) {
			for(ConfigRule rule: rules) {
				if (doneList.contains(rule)) {
					continue;
				}
				if (rule.match(config)) {
					doneList.add(rule);
					rule.fire(config);
					continue applyAllRules;
				}
			}
			break;
		}
	}
	
	private static class Index {

		Map<Object, Set<BeanConfig>> inverted = new HashMap<Object, Set<BeanConfig>>();
		
	}
	
	private static class Defered {
		
		BeanConfig config;
		Object obj;

		public Defered(BeanConfig config, Object obj) {
			this.config = config;
			this.obj = obj;
		}
	}
}
