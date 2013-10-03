package org.gridkit.vicluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class ViEngine {

	public enum Phase {
		PRE_INIT,
		POST_INIT,
		PRE_SHUTDOWN,
		PRE_KILL,
		POST_SHUTDOWN
	}
	
	public static String HOOK = ViConf.HOOK;
	public static String ACTIVATED_REMOTE_HOOK = ViConf.ACTIVATED_REMOTE_HOOK;
	public static String ACTIVATED_HOST_HOOK = ViConf.ACTIVATED_HOST_HOOK;
	
	public Map<String, Object> processPhase(Phase phase, Map<String, Object> config) {
		Game game = new Game(config);
		game.play(phase);
		return game.exportConfig();
	}

	public void executeHooks(ViExecutor target, Map<String, Object> config, boolean reverseOrder) {
		
		List<String> keySet = new ArrayList<String>(config.keySet());
		
		if (reverseOrder) {
			Collections.reverse(keySet);
		}
		
		for(String key: keySet) {
			if (key.startsWith(ACTIVATED_REMOTE_HOOK)) {
				Object hook = config.get(key);
				config.remove(key);
				if (hook != null) {
					if (hook instanceof Runnable) {
						target.exec((Runnable)hook);
					}
					else {
						throw new IllegalArgumentException("Hook " + key + " is not a Runnable");
					}
				}
			}
			else if (key.startsWith(ACTIVATED_HOST_HOOK)) {
				Object hook = config.get(key);
				config.remove(key);
				if (hook != null) {
					if (hook instanceof Runnable) {
						((Runnable)hook).run();
					}
					else {
						throw new IllegalArgumentException("Hook " + key + " is not a Runnable");
					}
				}
			}
		}
	}
	
	public interface QuorumGame {
		
		public String getStringProp(String propName);

		public Object getProp(String propName);

		public Map<String, Object> getConfigProps(String matcher);
		
		public void unsetProp(String propName);

		public void addUniqueProp(String propName, Object value);

		public void setProp(String propName, Object value);

		/**
		 * Will update value without reordering of keys.
		 */
		public void replaceProp(String propName, Object value);

		/**
		 * Will remove oldName placing newName in its place in key order.
		 */
		public void replaceProp(String oldName, String newName, Object value);
		
		public void rerunOnUpdate(Rerun rerun);

		public void rerunOnQuorum(Rerun rerun);
		
	}
	
	public interface Rerun {
		
		public void rerun(QuorumGame shift, Map<String, Object> changes);
		
	}
	
	public interface Interceptor {
		
		public void process(String name, Phase phase, QuorumGame game);
		
		public void processAddHoc(String name, ViNode node);
		
	}	

	private static class Game implements QuorumGame {

		List<String> keyOrder;
		Map<String, Object> config; 
		
		Set<String> executed = new HashSet<String>(); 
		List<RerunContext> updateRerunQueue = new ArrayList<RerunContext>();
		List<RerunContext> quorumRerunQueue = new ArrayList<RerunContext>();

		boolean dirty = false;
		long stepsLeft;

		public Game(Map<String, Object> config) {
			this.config = new HashMap<String, Object>(config);
			this.keyOrder = new ArrayList<String>(config.keySet());
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
				for(RerunContext ctx: quorumRerunQueue) {
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
		public Map<String, Object> getConfigProps(String matcher) {
			Pattern p = GlobHelper.translate(matcher, ".");
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			for(String key: keyOrder) {
				if (p.matcher(key).matches()) {
					if (config.get(key) != null) {
						map.put(key, config.get(key));
					}
				}
			}
			return Collections.unmodifiableMap(map);
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
	
	public static void addRule(QuorumGame game, InductiveRule rule) {
		game.rerunOnQuorum(new InductiveRuleHook(new AtomicBoolean(false), rule, false));
	}
	
	public static class InductiveRuleHook implements Rerun {
		
		private AtomicBoolean done;
		private InductiveRule rule;
		private boolean lastChance;
		
		public InductiveRuleHook(AtomicBoolean done, InductiveRule rule, boolean lastChance) {
			this.done = done;
			this.rule = rule;
			this.lastChance = lastChance;
		}

		@Override
		public void rerun(QuorumGame game, Map<String, Object> changes) {
			if (done.get()) {
				return;
			}
			if (rule.isApplicable(game) && rule.execute(game)) {
				done.set(true);
			}
			else {
				if (!lastChance) {
					lastChance = true;
				}
				game.rerunOnQuorum(this);
			}
		}
	}
	
	public static interface InductiveRule {

		public boolean isApplicable(QuorumGame game);
		
		public boolean execute(QuorumGame game);

	}
	
	public static class DefaultInitRuleSet implements Interceptor {

		@Override
		public void process(String name, Phase phase, QuorumGame game) {
			if (phase == Phase.PRE_INIT) {
				
			}
		}

		@Override
		public void processAddHoc(String name, ViNode node) {
			throw new IllegalStateException("Node '" + node + "' is already initialized"); 
		}
	}
	
	public static class TypeInitializerRule implements InductiveRule {

		@Override
		public boolean isApplicable(QuorumGame game) {
			String type = game.getStringProp(ViConf.NODE_TYPE);
			if (type == null) {
				return false;
			}
			else {
				return game.getProp(ViConf.TYPE_HANDLER + type) != null;
			}
		}

		@Override
		public boolean execute(QuorumGame game) {
			String type = game.getStringProp(ViConf.NODE_TYPE);
			InductiveRule rule = (InductiveRule) game.getProp(ViConf.TYPE_HANDLER + type);
			addRule(game, rule);
			return true;
		}
	}

	public static class ProcessLauncherRule implements InductiveRule {
		
		@Override
		public boolean isApplicable(QuorumGame game) {
			String type = game.getStringProp(ViConf.NODE_TYPE);
			if (type == null) {
				return false;
			}
			else {
				return game.getProp(ViConf.TYPE_HANDLER + type) != null;
			}
		}
		
		@Override
		public boolean execute(QuorumGame game) {
			String type = game.getStringProp(ViConf.NODE_TYPE);
			InductiveRule rule = (InductiveRule) game.getProp(ViConf.TYPE_HANDLER + type);
			addRule(game, rule);
			return true;
		}
	}
}
