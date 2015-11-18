package org.gridkit.nanocloud.telecontrol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.bootstraper.SystemHelper;

/**
 * {@link HostControlConsole} implementation for local execution.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class LocalControlConsole implements HostControlConsole {

	private List<Destroyable> activeObjects = new ArrayList<Destroyable>();
	private boolean terminated = false;
	private File cacheDir;
	
	private Map<String, String> hashCache = new HashMap<String, String>();
	
	public LocalControlConsole() {
		this("{tmp}/nanocloud");
	}

	public LocalControlConsole(String tmpDir) {
		try {
			cacheDir = new File(SystemHelper.normalizePath(tmpDir));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void ensureRunning() {
		if (terminated) {
			throw new IllegalStateException("Terminated");
		}
	}
	
	@Override
	public String cacheFile(FileBlob blob) {
		ensureRunning();
		try {
			File file = blob.getLocalFile();
			if (file != null && file.isFile()) {
				return file.getCanonicalPath(); 
			}
			else {		
				synchronized(hashCache) {
					String hashKey = blob.getContentHash() + "___" + blob.getFileName();
					if (hashCache.containsKey(hashKey)) {
						return hashCache.get(hashKey);
					}
					else {
						File f = new File(new File(cacheDir, blob.getContentHash()), blob.getFileName());
						f.getParentFile().mkdirs();
						File tmp = File.createTempFile(f.getName(), "", f.getParentFile());
						FileOutputStream fos = new FileOutputStream(tmp);
						StreamHelper.copy(blob.getContent(), fos);
						fos.close();
						tmp.renameTo(f);
						f.deleteOnExit();
						String path = f.getCanonicalPath();
						hashCache.put(hashKey, path);
						return path;
					}
				}
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> cacheFiles(List<? extends FileBlob> blobs) {
		ensureRunning();
		List<String> result = new ArrayList<String>();
		for(FileBlob blob: blobs) {
			result.add(cacheFile(blob));
		}
		return result;
	}

	@Override
	public Destroyable openSocket(SocketHandler handler) {
		ensureRunning();
		ServerSocket sock;
		try {
			sock = new ServerSocket();
			sock.setReuseAddress(true);
			sock.bind(new InetSocketAddress("127.0.0.1", 0), 1);
		} catch (SocketException e) {
			handler.terminated(e.toString());
			return new DestroyableStub();
		} catch (IOException e) {
			handler.terminated(e.toString());
			return new DestroyableStub();
		}
		
		SockerAcceptor accpector = new SockerAcceptor(sock, handler);
		Thread thread = new Thread(accpector);
		thread.setDaemon(false);
		thread.setName("ACCEPT[" + sock.getLocalSocketAddress() + "]");
		thread.start();
		
		return accpector;
	}

	@Override
	public Destroyable startProcess(String workingDir, String[] command, Map<String, String> env, ProcessHandler handler) {
		ensureRunning();
		ProcessObserver observer;
		try {
			Process process = startProcess(workingDir, command, env);
			observer = new ProcessObserver(process, handler);
			Thread thread = new Thread(observer);
			thread.setDaemon(false);
			thread.setName("EXEC[" + shortCmd(command) + "]." + process.hashCode());
			thread.start();
			return observer;
		} catch (IOException e) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();			
			PrintStream ps = new PrintStream(bos);
			ps.println("Failed to start process with command " + Arrays.asList(command));
			e.printStackTrace(ps);
			ps.flush();
			OutputStream stdIn = new ClosedOutputStream();
			InputStream stdOut = new ByteArrayInputStream(new byte[0]);
			InputStream stdErr = new ByteArrayInputStream(bos.toByteArray());
			handler.started(stdIn, stdOut, stdErr);
			handler.finished(Integer.MIN_VALUE);
			return new DestroyableStub();
		}
	}

	protected Process startProcess(String workingDir, String[] command,
			Map<String, String> env) throws IOException {
		File wd = workingDir == null ? null : new File(workingDir).getCanonicalFile();
		String[] envp = SystemHelper.buildInheritedEnvironment(env);
		Process process = Runtime.getRuntime().exec(command, envp, wd);
		return process;
	}
	
	protected String shortCmd(String[] cmd) {
		StringBuilder sb = new StringBuilder();
		if (cmd.length > 0) {
			String f = cmd[0];
			if (f.indexOf('/') > 0) {
				f = f.substring(f.lastIndexOf('/'));
			}
			if (f.indexOf('\\') > 0) {
				f = f.substring(f.lastIndexOf('\\'));
			}
			sb.append(f);
		}
		int n = 1;
		while(sb.length() < 40 && n < cmd.length) {
			sb.append(' ').append(cmd[n++]);
		}
		if (sb.length() > 40) {
			sb.setLength(40);
		}
		return sb.toString();
	}

	protected synchronized void register(Destroyable resource) {
		activeObjects.add(0, resource);
	}

	protected synchronized void unregister(Destroyable resource) {
		activeObjects.remove(resource);
	}
	
	@Override
	public void terminate() {
		terminated = true;
		List<Destroyable> killList;
		synchronized (this) {
			killList = new ArrayList<Destroyable>(activeObjects);
		}
		for(Destroyable d: killList) {
			d.destroy();
		}		
	}
	
	public File getCacheDir() {
	    return cacheDir;
	}

	private class SockerAcceptor implements Runnable, Destroyable {
		
		private ServerSocket serverSocket;
		private SocketHandler socketHandler;

		public SockerAcceptor(ServerSocket serverSocket, SocketHandler socketHandler) {
			this.serverSocket = serverSocket;
			this.socketHandler = socketHandler;
			register(this);
		}

		@Override
		public void run() {
			try {
				InetSocketAddress local = (InetSocketAddress) serverSocket.getLocalSocketAddress();
				Thread.currentThread().setName("ACCEPT[" + local + "]");
				socketHandler.bound(local.getHostName(), local.getPort());
				while(!serverSocket.isClosed()) {
					try {
						Socket socket = serverSocket.accept();
						InetSocketAddress remote = (InetSocketAddress) socket.getRemoteSocketAddress();
						socketHandler.accepted(remote.getHostName(), remote.getPort(), socket.getInputStream(), socket.getOutputStream());
					}
					catch(Exception e) {
						if (serverSocket.isClosed()) {
							sendDeathNote(e.toString());
						}
					}
				}
			}
			finally {
				sendDeathNote("");
				unregister(this); // FIXME socket is still open :( should handle it some how
			}
		}

		private void sendDeathNote(String message) {
			if (socketHandler != null) {
				socketHandler.terminated("");
				socketHandler = null;
			}
		}

		@Override
		public void destroy() {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// ignore
			}
			unregister(this);
		}		
	}
	
	private class ProcessObserver implements Runnable, Destroyable {
		
		private Process process;
		private ProcessHandler handler;
		
		public ProcessObserver(Process process, ProcessHandler handler) {
			this.process = process;
			this.handler = handler;
			register(this);
		}

		@Override
		public void run() {
			try {
				handler.started(process.getOutputStream(), process.getInputStream(), process.getErrorStream());
				while(true) {
					try {
						int exitCode = process.waitFor();
						handler.finished(exitCode);
						break;
					} catch (InterruptedException e) {
						// ignore interruption
					}
				}
			}
			finally{
				unregister(this);
			}
		}

		@Override
		public void destroy() {
			process.destroy();			
			unregister(this);
		}
	}
	
	private static class DestroyableStub implements Destroyable {
		@Override
		public void destroy() {
			// do nothing 
		}
	}
	
	private static class ClosedOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			throw new IOException("Stream closed");
		}
	}
}
