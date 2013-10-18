package org.gridkit.nanocloud.telecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.util.concurrent.TaskService;
import org.gridkit.vicluster.MassExec;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.ExecHandler;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.FileHandler;

public class TunnellerControlConsole implements HostControlConsole {

	private TunnellerConnection connection;
	private String cachePath;
	
	public TunnellerControlConsole(TunnellerConnection connection, String cachePath) {
		this.connection = connection;
		this.cachePath = cachePath;
	}

	protected FutureEx<String> pushFile(final FileBlob blob, final TaskService taskService) {
		final FutureBox<String> remotePath = new FutureBox<String>();
		try {
			connection.pushFile(cachePath + "/" + blob.getContentHash() + "/" + blob.getFileName(), new FileHandler() {
				
				@Override
				public void failed(String path, String error) {
					remotePath.setError(new RuntimeException("File upload failed: " + error));
				}
				
				@Override
				public void confirmed(String path, long size) {
					remotePath.setData(path);				
				}
				
				@Override
				public void accepted(final OutputStream out) {
					taskService.schedule(new TaskService.Task() {
						
						@Override
						public void run() {
							try {
								StreamHelper.copy(blob.getContent(), out);
								out.close();
							} catch (IOException e) {
								remotePath.setError(e);
							}
						}
						
						@Override
						public void interrupt(Thread taskThread) {
							// ignore, interruption is not supported
						}
						
						@Override
						public void canceled() {
							remotePath.setErrorIfWaiting(new RejectedExecutionException());
							
						}
					});
							
				}
			});
		} catch (IOException e) {
			remotePath.setError(e);
		}
		return remotePath;
	}

	@Override
	public String cacheFile(FileBlob blob) {
		FutureEx<String> f = pushFile(blob, SensibleTaskService.getShareInstance());
		return fget(f);
	}

	@Override
	public List<String> cacheFiles(List<? extends FileBlob> blobs) {
		TaskService ts = SensibleTaskService.getShareInstance();
		List<FutureEx<String>> paths = new ArrayList<FutureEx<String>>();
		for(FileBlob blob: blobs) {
			paths.add(pushFile(blob, ts));
		}
		return MassExec.collectAll(paths);
	}
	
	private static <T> T fget(Future<T> f) {
		try {
			return f.get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)(e.getCause());
			}
			else if (e.getCause() instanceof Error) {
				throw (Error)(e.getCause());
			} 
			else {
				throw new RuntimeException(e.getCause());
			}
		}
	}

	@Override
	public Destroyable openSocket(SocketHandler handler) {
		try {
			Sock sock = new Sock(handler);
			connection.newSocket(sock);
			return sock;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Destroyable startProcess(String workDir, String[] command, Map<String, String> env, ProcessHandler handler) {
		Proc proc;
		try {
			proc = new Proc(handler);
			proc.procId = connection.exec(workDir, command, env, proc);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return proc;
	}

	@Override
	public void terminate() {
		connection.close();
	}
	
	private class Proc implements ExecHandler, Destroyable {
		
		private ProcessHandler handler;
		private long procId = -1;

		public Proc(ProcessHandler handler) {
			this.handler = handler;
		}

		@Override
		public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
			handler.started(stdIn, stdOut, stdErr);
		}

		@Override
		public void finished(int exitCode) {
			handler.finished(exitCode);
		}

		@Override
		public void destroy() {
			try {
				connection.killProc(procId);
			} catch (IOException e) {
				// ignore
			}
		}
	}
	
	private static class Sock implements org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection.SocketHandler, Destroyable {

		private SocketHandler handler;
		
		public Sock(SocketHandler handler) {
			this.handler = handler;
		}

		@Override
		public void bound(String host, int port) {
			handler.bound(host, port);
		}

		@Override
		public void accepted(String remoteHost, int remotePort, InputStream soIn, OutputStream soOut) {
			handler.accepted(remoteHost, remotePort, soIn, soOut);
		}

		@Override
		public void destroy() {
			// TODO socket interruption for tunneller
		}
	}
}
