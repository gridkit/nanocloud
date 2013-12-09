package org.gridkit.nanocloud.telecontrol.isolate;

import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.GenericNodeTypeHandler;

public class IsolateNodeTypeHandler extends GenericNodeTypeHandler {
	
	@Override
	public boolean apply(QuorumGame game) {
		game.setProp(ViConf.PRAGMA_HANDLER + "isolate", new IsolatePragmaSupport());
		return super.apply(game);
	}

	protected String defaultJavaExecCmd(QuorumGame game) {
		return "[embeded]";
	}
	
	@Override
	protected ProcessLauncher createProcessLauncher(QuorumGame game) {
		IsolateLauncher launcher = new IsolateLauncher();
		game.setProp(ViConf.CONSOLE_STD_OUT_ECHO_STREAM, Isolate.getRootStdOut());
		game.setProp(ViConf.CONSOLE_STD_ERR_ECHO_STREAM, Isolate.getRootStdErr());
		return launcher;
	}

	protected HostControlConsole createControlConsole(QuorumGame game) {
		return new NoopConsole();
	}
	
	private static class NoopConsole implements HostControlConsole {

		@Override
		public String cacheFile(FileBlob blob) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> cacheFiles(List<? extends FileBlob> blobs) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Destroyable openSocket(SocketHandler handler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Destroyable startProcess(String workDir, String[] command, Map<String, String> env, ProcessHandler handler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void terminate() {
			throw new UnsupportedOperationException();
		}
	}
}
