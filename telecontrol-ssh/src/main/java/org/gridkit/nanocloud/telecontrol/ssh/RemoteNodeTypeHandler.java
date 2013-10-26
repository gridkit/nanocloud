package org.gridkit.nanocloud.telecontrol.ssh;

import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.telecontrol.LocalNodeTypeHandler;

public class RemoteNodeTypeHandler extends LocalNodeTypeHandler {

	@Override
	protected String defaultJavaExecCmd(QuorumGame game) {
		return null;
	}
	
	@Override
	protected void initExtraConfigurationRules(QuorumGame game) {
		super.initExtraConfigurationRules(game);
		// TODO rule or intercepter?
		game.rerunOnQuorum(new HostConfigurationInitializer.Runner());
	}

	@Override
	protected void initControlConsole(QuorumGame game) {
		ViEngine.Core.addRule(game, new RemoteConsoleInitializer());
	}
}
