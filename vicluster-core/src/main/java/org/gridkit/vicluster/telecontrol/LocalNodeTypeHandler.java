package org.gridkit.vicluster.telecontrol;

import java.io.File;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.LocalControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.ProcessSporeLauncher;
import org.gridkit.nanocloud.telecontrol.ZeroRmiRemoteSession;
import org.gridkit.vicluster.CloudContext;
import org.gridkit.vicluster.CloudContext.Helper;
import org.gridkit.vicluster.CloudContext.ServiceKey;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.InductiveRule;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.telecontrol.jvm.ProcessNodeFactory;
import org.gridkit.zerormi.hub.MasterHub;
import org.gridkit.zerormi.hub.RemotingHub;

public class LocalNodeTypeHandler implements ViEngine.InductiveRule {

	@Override
	public boolean apply(QuorumGame game) {
		game.setPropIfAbsent(ViConf.HOOK_CLASSPATH_BUILDER, createClasspathBuilder(game));
		game.setPropIfAbsent(ViConf.HOOK_JVM_ARGUMENTS_BUIDLER, createJvmArgumentsBuilder(game));
		game.setPropIfAbsent(ViConf.HOOK_JVM_ENV_VARS_BUIDLER, createJvmEnvironmentBuilder(game));
		game.setPropIfAbsent(ViConf.JVM_EXEC_CMD, defaultJavaExecCmd(game));

		game.setPropIfAbsent(ViConf.SPI_CONTROL_CONSOLE, createControlConsole(game));
		game.setPropIfAbsent(ViConf.SPI_REMOTING_HUB, createRemotingHub(game));
		game.setPropIfAbsent(ViConf.SPI_PROCESS_LAUNCHER, createProcessLauncher(game));
		game.setPropIfAbsent(ViConf.SPI_NODE_FACTORY, createNodeFactory(game));
		
		ViEngine.addRule(game, createRemotingConfigurationRule());
		ViEngine.addRule(game, createProcessLauncherRule());
		ViEngine.addRule(game, createNodeProducerRule());
		
		return true;
	}

	protected String defaultJavaExecCmd(QuorumGame game) {
		// this should work for Windows and Unix even though in Windows executable is java.exe
		File f  = new File(new File(new File(System.getProperty("java.home")), "bin"), "java");
		return f.getPath();
	}

	protected Interceptor createClasspathBuilder(QuorumGame game) {
		return new ViEngine.JvmClasspathReplicaBuilder();
	}

	protected Interceptor createJvmArgumentsBuilder(QuorumGame game) {
		return new ViEngine.JvmArgumentBuilder();
	}

	protected Interceptor createJvmEnvironmentBuilder(QuorumGame game) {
		// TODO
		return null;
	}

	protected HostControlConsole createControlConsole(QuorumGame game) {
		return game.getCloudContext().lookup(Helper.key(LocalControlConsole.class), CloudContext.Helper.reflectionProvider(LocalControlConsole.class, "terminate"));
	}

	protected MasterHub createRemotingHub(QuorumGame game) {
		return game.getCloudContext().lookup(Helper.key(RemotingHub.class), CloudContext.Helper.reflectionProvider(RemotingHub.class, "dropAllSessions"));
	}

	protected ProcessLauncher createProcessLauncher(QuorumGame game) {
		return game.getCloudContext().lookup(Helper.key(ProcessSporeLauncher.class), CloudContext.Helper.reflectionProvider(ProcessSporeLauncher.class, null));
	}

	protected NodeFactory createNodeFactory(QuorumGame game) {
		return game.getCloudContext().lookup(Helper.key(ProcessNodeFactory.class), CloudContext.Helper.reflectionProvider(ProcessNodeFactory.class, null));
	}

	protected InductiveRule createRemotingConfigurationRule() {
		return new ZeroRmiConfigurationRule();
	}

	protected InductiveRule createProcessLauncherRule() {
		return new ViEngine.ProcessLauncherRule();
	}

	protected InductiveRule createNodeProducerRule() {
		return new ViEngine.NodeProductionRule();
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
}
