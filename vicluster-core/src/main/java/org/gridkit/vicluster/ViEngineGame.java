package org.gridkit.vicluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.ViEngine.Rerun;
import org.gridkit.vicluster.ViEngine.SpiPropsWrapper;

class ViEngineGame extends SpiPropsWrapper implements QuorumGame {

	List<String> keyOrder;
	Map<String, Object> config;
	ViConf spiConf;
	
	Set<String> executed = new HashSet<String>(); 
	List<RerunContext> updateRerunQueue = new ArrayList<RerunContext>();
	List<RerunContext> quorumRerunQueue = new ArrayList<RerunContext>();

	boolean dirty = false;
	long stepsLeft;

	public ViEngineGame(Map<String, Object> config) {
		this.config = new HashMap<String, Object>(config);
		this.spiConf = new ViConf(Collections.unmodifiableMap(this.config));
		this.keyOrder = new ArrayList<String>(config.keySet());
	}
	
	@Override
	protected ViSpiConfig getConfig() {
		return spiConf;
	}

	public void play(Phase phase) {
		while(true) {
			String key = pickInterceptor();
			if (key == null) {
				break;
			}
			Interceptor interceptor = (Interceptor) config.get(key);
			interceptor.process(key, phase, this);
		}
		stepsLeft = 2 * executed.size() * executed.size();
		quorum:
		while(true) {
			for(RerunContext ctx: updateRerunQueue) {
				if (!ctx.changes.isEmpty()) {
					updateRerunQueue.remove(ctx);
					incRunCount();
					ctx.closure.rerun(this, ctx.changes);
					continue quorum;
				}
			}
			// initial quorum reached
			dirty = false;
			while(!quorumRerunQueue.isEmpty()) {
				Iterator<RerunContext> it = quorumRerunQueue.iterator();
				RerunContext ctx = it.next();
				it.remove();
				incRunCount();
				ctx.closure.rerun(this, ctx.changes);
				if (dirty) {
					continue quorum;
				}
			}
			break;
		}
	}
	
	private void incRunCount() {
		if (--stepsLeft < 0) {
			throw new RuntimeException("Cannot reach configuration quorum, attempt limit reached");
		}
	}

	private String pickInterceptor() {
		for(String key: keyOrder) {
			if (key.startsWith("hook:")) {
				if (!executed.contains(key)) {
					executed.add(key);
					Object val = config.get(key);
					if (val instanceof Interceptor) {
						return key;
					}
					if (val != null) {
						throw new IllegalArgumentException("Illegal hook object " + key + " -> " + val);
					}
				}
			}
		}			
		return null;
	}

	@Override
	public String getStringProp(String propName) {
		return (String) config.get(propName);
	}

	@Override
	public Object getProp(String propName) {
		return config.get(propName);
	}

	@Override
	public Map<String, Object> getConfigProps(String prefix) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for(String key: keyOrder) {
			if (key.startsWith(prefix)) {
				if (config.get(key) != null) {
					map.put(key, config.get(key));
				}
			}
		}
		return Collections.unmodifiableMap(map);
	}

	@Override
	public Map<String, Object> getAllConfigProps() {
		return Collections.unmodifiableMap(config);
	}

	@Override
	public void setProp(String propName, Object value) {
		if (value == null) {
			unsetProp(propName);
		}
		else {
			keyOrder.remove(propName);
			keyOrder.add(propName);
			config.put(propName, value);
			broadcastChange(propName, value);
		}
	}

	@Override
	public void setPropIfAbsent(String propName, Object value) {
		if (config.get(propName) == null) {
			if (value == null) {
				unsetProp(propName);
			}
			else {
				keyOrder.remove(propName);
				keyOrder.add(propName);
				config.put(propName, value);
				broadcastChange(propName, value);
			}
		}
	}

	@Override
	public void addUniqueProp(String propName, Object value) {
		if (config.containsKey(propName)) {
			int n = 1;
			while(config.containsKey(propName + "." + (++n)));
			setProp(propName + "." + n, value);
		}		
		else {
			setProp(propName, value);
		}
	}

	@Override
	public void replaceProp(String propName, Object value) {
		if (value == null) {
			unsetProp(propName);
		}
		else {
			int n = keyOrder.indexOf(propName);
			if (n < 0) {
				throw new IllegalArgumentException("Cannot replace, key '" + propName + "' is missing");
			}
			config.put(propName, value);
			broadcastChange(propName, value);
		}			
	}

	@Override
	public void replaceProp(String oldName, String newName, Object value) {
		if (value == null) {
			unsetProp(oldName);
			unsetProp(newName);
		}
		else if (oldName.equals(newName)) {
			replaceProp(oldName, value);
		}
		else {
			int n = keyOrder.indexOf(oldName);
			if (n < 0) {
				throw new IllegalArgumentException("Cannot replace, key '" + oldName + "' is missing");
			}
			config.remove(oldName);
			config.put(newName, value);
			keyOrder.set(n, newName);
			broadcastChange(oldName, null);
			broadcastChange(newName, value);
		}
	}

	@Override
	public void unsetProp(String propName) {
		if (config.get(propName) != null) {
			broadcastChange(propName, null);
			config.put(propName, null);
		}
	}

	private void broadcastChange(String propName, Object object) {
		if (executed.contains(propName)) {
			throw new IllegalStateException("Interceptor '" + propName + "' is already activated");
		}
		dirty = true;
		for (RerunContext ctx: updateRerunQueue) {
			ctx.changes.put(propName, object);
		}			
		for (RerunContext ctx: quorumRerunQueue) {
			ctx.changes.put(propName, object);
		}
	}

	@Override
	public void rerunOnUpdate(Rerun rerun) {
		updateRerunQueue.add(new RerunContext(rerun));
	}

	@Override
	public void rerunOnQuorum(Rerun rerun) {
		quorumRerunQueue.add(new RerunContext(rerun));
	}

	public Map<String, Object> exportConfig() {
		HashMap<String, Object> map = new LinkedHashMap<String, Object>(keyOrder.size());
		for(String key: keyOrder) {
			map.put(key, config.get(key));
		}
		return map;
	}
	
	private class RerunContext {
		
		Map<String, Object> changes = new LinkedHashMap<String, Object>();
		Rerun closure;
		
		public RerunContext(Rerun closure) {
			this.closure = closure;
		}
	}
}