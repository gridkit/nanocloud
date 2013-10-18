package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gridkit.internal.com.jcraft.jsch.ChannelExec;
import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.nanocloud.telecontrol.LocalControlConsole;
import org.gridkit.vicluster.telecontrol.ExecCommand;
import org.gridkit.vicluster.telecontrol.FileBlob;

public class SshHostControlConsole extends LocalControlConsole {

	private Session session;
	private String resourceCachePath;
	private String resolvedPath;
	
	public SshHostControlConsole(Session session, String resourceCachePath) {
		this.session = session;
		this.resourceCachePath = resourceCachePath;
		register(new SessionKiller(session));
	}

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



	private class ProcessObserver implements Runnable, Destroyable {
		
		private ChannelExec process;
		private ProcessHandler handler;
		private boolean notified;
		
		public ProcessObserver(ChannelExec process, ProcessHandler handler) {
			this.process = process;
			this.handler = handler;
			register(this);
		}

		@Override
		public void run() {
			try {
				try {
					handler.started(process.getOutputStream(), process.getInputStream(), process.getExtInputStream());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				while(true) {
					if (process.isClosed()) {
						break;
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				synchronized(this) {
					if (!notified) {
						notified = true;
						handler.finished(process.getExitStatus());
					}
				}
			}
			finally{
				destroy();
			}
		}

		@Override
		public void destroy() {
			synchronized(this) {
				if (!process.isClosed()) {
					try {
						process.sendSignal("KILL");
					} catch (Exception e) {
					}
					long dl = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200);
					while(System.nanoTime() < dl) {
						if (process.isClosed()) {
							return;
						}
						else {
							try {
								Thread.sleep(10);
							}
							catch(InterruptedException e) {
								break;
							}
						}
					}
				}

				if (!notified) {
					notified = true;
					int exitCode = process.getExitStatus();
					handler.finished(exitCode);
				}
				try {
					process.disconnect();
				}
				catch(Exception e) {
					// ignore
				}
			}
			unregister(this);
		}
	}
	
	private class ProcessKiller implements Destroyable {
		
		private final ChannelExec channel;
		private final ProcessHandler handler;
		private boolean notified;
		
		public ProcessKiller(ChannelExec channel, ProcessHandler handler) {
			this.channel = channel;
			this.handler = handler;
		}

		@Override
		public void destroy() {
			synchronized(this) {
				if (!notified) {
					notified = true;
					if (!channel.isClosed()) {
						try {
							channel.sendSignal("KILL");
						} catch (Exception e) {
						}
						long dl = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200);
						while(System.nanoTime() < dl) {
							if (channel.isClosed()) {
								return;
							}
							else {
								try {
									Thread.sleep(10);
								}
								catch(InterruptedException e) {
									break;
								}
							}
						}
					}
					
					int exitCode = channel.getExitStatus();
					handler.finished(exitCode);
					try {
						channel.disconnect();
					}
					catch(Exception e) {
						// ignore
					}
				}			
			}
			unregister(this);
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
	
	private static class DestroyableStub implements Destroyable {

		@Override
		public void destroy() {
		}
	}
}
