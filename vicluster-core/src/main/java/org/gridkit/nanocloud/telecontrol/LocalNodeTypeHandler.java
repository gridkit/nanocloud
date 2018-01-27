package org.gridkit.nanocloud.telecontrol;

import java.io.File;

import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.telecontrol.GenericNodeTypeHandler;

public class LocalNodeTypeHandler extends GenericNodeTypeHandler {
	
	protected String defaultJavaExecCmd(QuorumGame game) {
		// this should work for Windows and Unix even though in Windows executable is java.exe
		File f  = new File(new File(new File(System.getProperty("java.home")), "bin"), "java");
		return f.getPath();
	}
	
	protected HostControlConsole createControlConsole(QuorumGame game) {
		return getCloudSingleton(game, LocalControlConsole.class, "terminate");
	}

	@Override
	protected Interceptor createClasspathBuilder(QuorumGame game) {
		return new ViEngine.RuleSet(new ShallowClasspathBuilder(), new ClasspathReplicaBuilderLocal());
	}
}
