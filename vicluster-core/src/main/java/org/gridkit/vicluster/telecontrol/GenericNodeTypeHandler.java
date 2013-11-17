package org.gridkit.vicluster.telecontrol;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.ProcessSporeLauncher;
import org.gridkit.nanocloud.telecontrol.ZeroRmiRemoteSession;
import org.gridkit.vicluster.CloudContext;
import org.gridkit.vicluster.CloudContext.Helper;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.IdempotentConfigBuilder;
import org.gridkit.vicluster.ViEngine.InductiveRule;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;

public abstract class GenericNodeTypeHandler implements ViEngine.InductiveRule {

	@Override
	public boolean apply(QuorumGame game) {
		initJvmConfigurationRules(game);
		initExtraConfigurationRules(game);
		initControlConsole(game);
		initProcessLauncher(game);
		
		initRemoting(game);
		initProcessBootstrapper(game);
		
		return true;
	}

	protected void initExtraConfigurationRules(QuorumGame game) {		
	}

	protected void initProcessBootstrapper(QuorumGame game) {
		ViEngine.Core.addRule(game, createProcessBootstrapperRule());
	}

	protected void initRemoting(QuorumGame game) {
		ViEngine.Core.addRule(game, createRemotingConfigurationRule());
	}

	protected void initProcessLauncher(QuorumGame game) {
		game.setPropIfAbsent(ViConf.SPI_PROCESS_LAUNCHER, createProcessLauncher(game));
	}

	protected void initControlConsole(QuorumGame game) {
		game.setPropIfAbsent(ViConf.SPI_CONTROL_CONSOLE, createControlConsole(game));
	}

	protected void initJvmConfigurationRules(QuorumGame game) {
		game.setPropIfAbsent(ViConf.HOOK_CLASSPATH_BUILDER, createClasspathBuilder(game));
		game.setPropIfAbsent(ViConf.HOOK_JVM_ARGUMENTS_BUIDLER, createJvmArgumentsBuilder(game));
		game.setPropIfAbsent(ViConf.HOOK_JVM_ENV_VARS_BUIDLER, createJvmEnvironmentBuilder(game));
		game.setPropIfAbsent(ViConf.JVM_EXEC_CMD, defaultJavaExecCmd(game));
	}

	protected abstract String defaultJavaExecCmd(QuorumGame game);

	protected Interceptor createClasspathBuilder(QuorumGame game) {
		return new JvmClasspathReplicaBuilder();
	}

	protected Interceptor createJvmArgumentsBuilder(QuorumGame game) {
		return new JvmArgumentBuilder();
	}

	protected Interceptor createJvmEnvironmentBuilder(QuorumGame game) {
		// TODO
		return null;
	}

	protected <T> T getCloudSingleton(QuorumGame game, Class<T> type, String shutdownMethod) {
		T instance = game.getCloudContext().lookup(Helper.key(type), CloudContext.Helper.reflectionProvider(type, shutdownMethod));
		return instance;
	}
	
	protected abstract HostControlConsole createControlConsole(QuorumGame game);
	
	protected ProcessLauncher createProcessLauncher(QuorumGame game) {
		return getCloudSingleton(game, ProcessSporeLauncher.class, null);
	}

	protected InductiveRule createRemotingConfigurationRule() {
		return new ZeroRmiConfigurationRule();
	}

	protected InductiveRule createProcessBootstrapperRule() {
		return new ProcessLauncherRule();
	}

	protected InductiveRule createNodeProducerRule() {
		return new NodeProductionRule();
	}
	
	public static class ZeroRmiConfigurationRule implements InductiveRule {

		@Override
		public boolean apply(QuorumGame game) {
			if (	game.getRemotingSession() == null
				&&	game.getProcessLauncher() != null
				&&  game.getControlConsole() != null) {
			
				String nodeName = game.getStringProp(ViConf.NODE_NAME);
				ZeroRmiRemoteSession session = new ZeroRmiRemoteSession(nodeName);
				game.setProp(ViConf.SPI_REMOTING_SESSION, session);
				
				return true;
			}
			else {
				return false;
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
				final ManagedProcess mp = launcher.createProcess(game.getConfigProps(""));
				game.setProp(ViConf.SPI_MANAGED_PROCESS, mp);
				game.addUniqueProp(ViConf.ACTIVATED_FINALIZER_HOOK + "destroy-process", new Runnable() {
					@Override
					public void run() {
						mp.destroy();
					}
				});
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
}
