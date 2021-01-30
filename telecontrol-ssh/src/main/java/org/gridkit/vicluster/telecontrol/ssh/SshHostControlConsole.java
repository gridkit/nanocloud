package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.gridkit.nanocloud.telecontrol.LocalControlConsole;
import org.gridkit.vicluster.telecontrol.ExecCommand;
import org.gridkit.vicluster.telecontrol.FileBlob;

public class SshHostControlConsole extends LocalControlConsole {

	private Session session;
	private SftFileCache fileCache;
	
	public SshHostControlConsole(Session session, String cachePath, boolean useRelativePaths, int sftpParallelFactor) {
		try {
			this.session = session;
			this.fileCache = new SftFileCache(session, cachePath, useRelativePaths, sftpParallelFactor);
			register(new CacheKiller(fileCache));
			register(new SessionKiller(session));
		} catch (JSchException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isLocalFileSystem() {
		return false;
	}

	@Override
	public String cacheFile(FileBlob blob) {
		return fileCache.upload(blob);
	}

	@Override
	public List<String> cacheFiles(List<? extends FileBlob> blobs) {
		return fileCache.upload(blobs);
	}

	@Override
	public Destroyable openSocket(SocketHandler handler) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Process startProcess(String workingDir, String[] command, Map<String, String> env) throws IOException {
		ExecCommand cmd = new ExecCommand(command[0]);
		for(int i = 1; i != command.length; ++i) {
			cmd.addArg(command[i]);
		}
		cmd.setWorkDir(workingDir);
		if (env != null) {
			for(String key: env.keySet()) {
				if (env.get(key) != null) {
					cmd.setEnvironment(key, env.get(key));
				}
			}
		}
		try {
			return new RemoteSshProcess(session, cmd);
		} catch (JSchException e) {
			throw new IOException(e);
		}
	}
	
	private static class SessionKiller implements Destroyable {

		private final Session session;
		
		public SessionKiller(Session session) {
			this.session = session;
		}

		@Override
		public void destroy() {
			session.disconnect();
		}		
	}

	private static class CacheKiller implements Destroyable {
		
		private final SftFileCache fileCache;
		
		public CacheKiller(SftFileCache fileCache) {
			this.fileCache = fileCache;
		}
		
		@Override
		public void destroy() {
			fileCache.close();
		}		
	}	
}
