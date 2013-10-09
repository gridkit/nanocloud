package org.gridkit.vicluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.vicluster.CloudContext.ServiceKey;
import org.gridkit.vicluster.CloudContext.ServiceProvider;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

public interface ViEngine {

	public enum Phase {
		PRE_INIT,
		POST_INIT,
		PRE_SHUTDOWN,
		PRE_KILL,
		POST_SHUTDOWN
	}
	
	public static class Core implements ViEngine {
	
		public Map<String, Object> processPhase(Phase phase, Map<String, Object> config) {
			ViEngineGame game = new ViEngineGame(config);
			game.play(phase);
			return game.exportConfig();
		}

		public void executeHooks(ViExecutor target, Map<String, Object> config, boolean reverseOrder) {
			
			List<String> keySet = new ArrayList<String>(config.keySet());
			
			if (reverseOrder) {
				Collections.reverse(keySet);
			}
			
			for(String key: keySet) {
				if (key.startsWith(ViConf.ACTIVATED_REMOTE_HOOK)) {
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
				else if (key.startsWith(ViConf.ACTIVATED_HOST_HOOK)) {
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
		
		public static ViSpiConfig asSpiConfig(final Map<String, Object> config) {
			return new ViConf(config);
		}

		public static void addRule(QuorumGame game, InductiveRule rule) {
			game.rerunOnQuorum(new InductiveRuleHook(new AtomicBoolean(false), rule, false));
		}

		public static <T> Interceptor newSingletonInjector(final String configKey, final ServiceKey<T> key, final ServiceProvider<T> provider) {
			return new Interceptor() {
				
				@Override
				public void process(String name, Phase phase, QuorumGame game) {
					CloudContext context = game.getCloudContext();
					T service = provider == null ? context.lookup(key) : context.lookup(key, provider);
					if (service != null) {
						game.setProp(configKey, service);
					}
				}
				
				@Override
				public void processAddHoc(String name, ViNode node) {
					throw new IllegalArgumentException("Node is already initialized");
				}
			};
		}
	}
	
	public static abstract class SpiPropsWrapper implements ViSpiConfig {
		
		protected abstract ViSpiConfig getConfig();

		public CloudContext getCloudContext() {
			return getConfig().getCloudContext();
		}

		public HostControlConsole getControlConsole() {
			return getConfig().getControlConsole();
		}

		public ProcessLauncher getProcessLauncher() {
			return getConfig().getProcessLauncher();
		}

		public RemoteExecutionSession getRemotingSession() {
			return getConfig().getRemotingSession();
		}

		public String getJvmExecCmd() {
			return getConfig().getJvmExecCmd();
		}

		public List<ClasspathEntry> getJvmClasspath() {
			return getConfig().getJvmClasspath();
		}

		public List<String> getJvmArgs() {
			return getConfig().getJvmArgs();
		}

		public ManagedProcess getManagedProcess() {
			return getConfig().getManagedProcess();
		}

		public NodeFactory getNodeFactory() {
			return getConfig().getNodeFactory();
		}

		public ViNode getNodeInstance() {
			return getConfig().getNodeInstance();
		}
	}
	
	public interface QuorumGame extends ViSpiConfig {
		
		public String getStringProp(String propName);

		void setPropIfAbsent(String propName, Object value);

		Map<String, Object> getAllConfigProps();

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
		
		public void rerun(QuorumGame game, Map<String, Object> changes);
		
	}
	
	public interface Interceptor {
		
		public void process(String name, Phase phase, QuorumGame game);
		
		public void processAddHoc(String name, ViNode node);
		
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
			if (changes != null) {
				lastChance = false;
			}
			if (done.get()) {
				return;
			}
			if (rule.apply(game)) {
				done.set(true);
			}
			else {
				game.rerunOnUpdate(this);
				if (!lastChance) {
					lastChance = true;
				}
				else {
					game.rerunOnQuorum(this);
				}
			}
		}
	}
	
	public static interface InductiveRule {

		public boolean apply(QuorumGame game);

	}
	
	public static class DefaultInitRuleSet implements Interceptor {

		@Override
		public void process(String name, Phase phase, QuorumGame game) {
			if (phase == Phase.PRE_INIT) {
				
				String type = game.getStringProp(ViConf.NODE_TYPE);
				if (type == null) {
					throw new IllegalArgumentException("Node type is not defined");
				}
				InductiveRule rule = (InductiveRule) game.getProp(ViConf.TYPE_HANDLER + type);
				if (rule == null) {
					throw new IllegalArgumentException("Handler for type '" + type + "' is not found");
				}
				if (!rule.apply(game)) {
					throw new IllegalArgumentException("Type initilizer " + rule + " has failed");
				}				
			}
		}

		@Override
		public void processAddHoc(String name, ViNode node) {
			throw new IllegalStateException("Node '" + node + "' is already initialized"); 
		}
	}
	
	public static class TypeInitializerRule implements InductiveRule {

		@Override
		public boolean apply(QuorumGame game) {
			String type = game.getStringProp(ViConf.NODE_TYPE);
			if (type == null) {
				return false;
			}
			InductiveRule rule = (InductiveRule) game.getProp(ViConf.TYPE_HANDLER + type);
			if (rule == null) {
				return false;
			}
			Core.addRule(game, rule);
			return true;
		}
	}

	
	public static abstract class IdempotentConfigBuilder<T> implements Interceptor, Rerun {
		
		protected String configKey;
		
		protected abstract T buildState(QuorumGame game);
		
		protected boolean sameState(T oldState, T newState) {
			return oldState == null ? newState == null : oldState.equals(newState);
		}

		public IdempotentConfigBuilder(String configKey) {
			this.configKey = configKey;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void rerun(QuorumGame game, Map<String, Object> changes) {
			game.rerunOnUpdate(this);
			if (!changes.isEmpty()) {
				T oldV = (T)game.getProp(configKey);
				T newV = buildState(game);
				if (!sameState(oldV, newV)) {
					game.setProp(configKey, buildState(game));					
				}
			}			
		}

		@Override
		public void process(String name, Phase phase, QuorumGame game) {
			if (phase == Phase.PRE_INIT) {
				game.setProp(configKey, buildState(game));
				game.rerunOnUpdate(this);
			}			
		}

		@Override
		public void processAddHoc(String name, ViNode node) {
			throw new IllegalArgumentException("Node is already initialized");
		}
	}
		
	public static abstract class ControlConsoleInitialzer implements InductiveRule {

		@Override
		public boolean apply(QuorumGame game) {
			game.setPropIfAbsent(ViConf.SPI_CONTROL_CONSOLE, getControlConsole());
			return true;
		}
		
		protected abstract HostControlConsole getControlConsole();
		
	}
}
