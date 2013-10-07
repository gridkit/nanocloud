package org.gridkit.vicluster;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.vicluster.CloudContext.ServiceKey;
import org.gridkit.vicluster.CloudContext.ServiceProvider;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.zerormi.hub.MasterHub;

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
	
	public interface SpiContext {
		
		public CloudContext getCloudContext();
		
		public HostControlConsole getControlConsole();

		public ProcessLauncher getProcessLauncher();

		public MasterHub getMasterHub();
		
		public RemoteExecutionSession getRemotingSession();
		
		public String getJvmExecCmd();

		public List<ClasspathEntry> getJvmClasspath();
		
		public List<String> getJvmArgs();
		
		public ManagedProcess getManagedProcess();
		
		public NodeFactory getNodeFactory();
		
		public ViNode getNodeInstance();
		
	}
	
	public static SpiContext asSpiConfig(final Map<String, Object> config) {
		return new SpiPropsWrapper() {
			@Override
			protected Map<String, Object> getConfig() {
				return config;
			}
		};
	}
	
	public static abstract class SpiPropsWrapper implements SpiContext {
		
		protected abstract Map<String, Object> getConfig();
		
		@Override
		public CloudContext getCloudContext() {
			return (CloudContext) getConfig().get(ViConf.SPI_CLOUD_CONTEXT);
		}

		@Override
		public HostControlConsole getControlConsole() {
			return (HostControlConsole) getConfig().get(ViConf.SPI_CONTROL_CONSOLE);
		}

		@Override
		public ProcessLauncher getProcessLauncher() {
			return (ProcessLauncher) getConfig().get(ViConf.SPI_PROCESS_LAUNCHER);
		}

		@Override
		public MasterHub getMasterHub() {
			return (MasterHub) getConfig().get(ViConf.SPI_REMOTING_HUB);
		}
		
		@Override
		public RemoteExecutionSession getRemotingSession() {
			return (RemoteExecutionSession) getConfig().get(ViConf.SPI_REMOTING_SESSION);
		}

		@Override
		public String getJvmExecCmd() {
			return (String) getConfig().get(ViConf.JVM_EXEC_CMD);
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<ClasspathEntry> getJvmClasspath() {
			return (List<ClasspathEntry>) getConfig().get(ViConf.SPI_JVM_CLASSPATH);
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<String> getJvmArgs() {
			return (List<String>) getConfig().get(ViConf.SPI_JVM_ARGS);
		}

		@Override
		public ManagedProcess getManagedProcess() {
			return (ManagedProcess) getConfig().get(ViConf.SPI_MANAGED_PROCESS);
		}

		@Override
		public NodeFactory getNodeFactory() {
			return (NodeFactory) getConfig().get(ViConf.SPI_NODE_FACTORY);
		}

		@Override
		public ViNode getNodeInstance() {
			return (ViNode) getConfig().get(ViConf.SPI_NODE_INSTANCE);
		}
	}
	
	public interface QuorumGame extends SpiContext {
		
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

	private static class Game extends SpiPropsWrapper implements QuorumGame {

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
		
		@Override
		protected Map<String, Object> getConfig() {
			return config;
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
			addRule(game, rule);
			return true;
		}
	}

	public static class ProcessLauncherRule implements InductiveRule {
		
		@Override
		public boolean apply(QuorumGame game) {
			if (
					game.getManagedProcess() == null
				&&  game.getControlConsole() != null
				&&  game.getRemotingSession() != null
				&&  game.getProcessLauncher() != null
				&&  game.getJvmExecCmd() != null
				&&  game.getJvmClasspath() != null
				&&  game.getJvmArgs() != null)
			{
				ProcessLauncher launcher = (ProcessLauncher) game.getProp(ViConf.SPI_PROCESS_LAUNCHER);
				ManagedProcess mp = launcher.createProcess(game.getConfigProps("**"));
				game.setProp(ViConf.SPI_MANAGED_PROCESS, mp);
				return true;
			}
			else {
				return false;
			}
		}
	}

	public static class NodeProductionRule implements InductiveRule {
		
		@Override
		public boolean apply(QuorumGame game) {
			if (
					game.getNodeInstance() == null
				&&  game.getNodeFactory() != null
				&&  game.getManagedProcess() != null
				)
			{
				NodeFactory factory = game.getNodeFactory();
				ViNode mp = factory.createViNode(game.getAllConfigProps());
				game.setProp(ViConf.SPI_NODE_INSTANCE, mp);
				return true;
			}
			else {
				return false;
			}
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
	
	public static class JvmArgumentBuilder extends IdempotentConfigBuilder<List<String>> {

		public JvmArgumentBuilder() {
			super(ViConf.SPI_JVM_ARGS);
		}
		
		@Override
		protected List<String> buildState(QuorumGame game) {
			Map<String, Object> cmd = game.getConfigProps(ViConf.JVM_ARGUMENT);
			List<String> options = new ArrayList<String>();
			for(Object v: cmd.values()) {
				String o = (String) v;
				if (o.startsWith("|")) {
					String[] opts = o.split("\\|");
					for(String oo: opts) {
						addOption(options, oo);
					}
				}
				else {
					addOption(options, o);
				}
			}
			return options;
		}
		
		private void addOption(List<String> options, String o) {
			o = o.trim();
			if (o.length() > 0) {
				options.add(o);
			}
		}
	}

	public static class JvmClasspathReplicaBuilder extends IdempotentConfigBuilder<List<ClasspathEntry>> {

		public JvmClasspathReplicaBuilder() {
			super(ViConf.SPI_JVM_CLASSPATH);
		}

		
		@Override
		protected List<ClasspathEntry> buildState(QuorumGame game) {
			try {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Map<String, String> tweaks = (Map<String, String>) (Map) game.getConfigProps(ViConf.CLASSPATH_TWEAK);
				List<ClasspathEntry> cp = Classpath.getClasspath(Thread.currentThread().getContextClassLoader());
				if (tweaks.isEmpty()) {
					return cp;
				}
				else {
					List<ClasspathEntry> entries = new ArrayList<Classpath.ClasspathEntry>(cp);
					
					for(String change: tweaks.values()) {
						if (change.startsWith("+")) {
							String cpe = normalize(change.substring(1));
							addEntry(entries, cpe);
						}
						else if (change.startsWith("-")) {
							String cpe = normalize(change.substring(1));
							removeEntry(entries, cpe);
						}
					}
					
					return entries;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		private void addEntry(List<ClasspathEntry> entries, String path) throws IOException {
			ClasspathEntry entry = Classpath.getLocalEntry(path);
			if (entry != null) {
				entries.add(entry);
			}
		}

		private void removeEntry(List<ClasspathEntry> entries, String path) {
			Iterator<ClasspathEntry> it = entries.iterator();
			while(it.hasNext()) {
				if (path.equals(normalize(it.next().getUrl()))) {
					it.remove();
				}
			}
		}
				
		private String normalize(String path) {
			try {
				// normalize path entry if possible
				return new File(path).getCanonicalPath();
			} catch (IOException e) {
				return path;
			}
		}
		
		private String normalize(URL url) {
			try {
				if (!"file".equals(url.getProtocol())) {
					throw new IllegalArgumentException("Non file URL in classpath: " + url);
				}
				File f = new File(url.toURI());
				String path = f.getPath();
				return normalize(path);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Malformed URL in classpath: " + url);
			}
		}
				
		@Override
		protected boolean sameState(List<ClasspathEntry> existing, List<ClasspathEntry> rebuilt) {
			if (existing.size() != rebuilt.size()) {
				return false;
			}
			else {
				for(int i = 0; i != existing.size(); ++i) {
					ClasspathEntry e1 = existing.get(0);
					ClasspathEntry e2 = rebuilt.get(0);
					if (e1.getLocalFile() == null && e2.getLocalFile() == null) {
						if (!compareContent(e1, e2)) {
							return false;
						}
					}
					else if (e1.getLocalFile() == null || e2.getLocalFile() == null) {
						return false;
					}
					else {
						File p1 = e1.getLocalFile();
						File p2 = e2.getLocalFile();
						if (!p1.getPath().equals(p2.getPath())) {
							return false;
						}
					}
				}
			}
			return true;
		}

		private boolean compareContent(ClasspathEntry e1, ClasspathEntry e2) {
			return e1.getContentHash().equals(e2.getContentHash());
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
