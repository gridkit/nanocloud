package org.gridkit.vicluster.telecontrol;

import java.io.File;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.LocalControlConsole;
import org.gridkit.vicluster.ViEngine.QuorumGame;

public class LocalNodeTypeHandler extends GenericNodeTypeHandler {
	
	protected String defaultJavaExecCmd(QuorumGame game) {
		// this should work for Windows and Unix even though in Windows executable is java.exe
		File f  = new File(new File(new File(System.getProperty("java.home")), "bin"), "java");
		return f.getPath();
	}
	
	protected HostControlConsole createControlConsole(QuorumGame game) {
		return getCloudSingleton(game, LocalControlConsole.class, "terminate");
	}

}
