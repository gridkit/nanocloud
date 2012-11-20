package org.gridkit.lab.util.shell;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Shell implements Serializable {

	private static final long serialVersionUID = 20121118L;
	
	private Map<String, String> env = new HashMap<String, String>();
	
	public static Shell get() {
		return new Shell();
	}
	
	public Shell env(String var, String value) {
		env.put(var, value);
		return this;
	}
	
	
	public void exec(String... command) throws IOException, InterruptedException {
		int ex = syncProcExec(command);
		if (ex != 0) {
			throw new NonZeroExitCodeException("Exit code: " + ex); 
		}
	}

	public void exec(File path, String... command) throws IOException, InterruptedException {
		int ex = syncProcExec(path, command);
		if (ex != 0) {
			throw new NonZeroExitCodeException("Exit code: " + ex); 
		}
	}
	
	public int syncProcExec(String... command) throws IOException, InterruptedException {
		return syncProcExec(new File("."), command);
	}

	public int syncProcExec(File path, String... command) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(command);
		if (!".".equals(path.getPath())) {
			pb.directory(path);
		}

		pb.environment().putAll(env);
		
		Process p = pb.start();		
		InputStream stdOut = p.getInputStream();
		InputStream stdErr = p.getErrorStream();
		p.getOutputStream().close();
		while(isAlive(p)) {
			if (!pump(stdOut, System.out) & !pump(stdErr, System.err)) {
				Thread.sleep(100);
			}
		}
		pump(stdOut, System.out);
		pump(stdErr, System.err);
		return p.exitValue();
	}

	public Input execWithInput(File path, String... command) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(command);
		if (!".".equals(path.getPath())) {
			pb.directory(path);
		}
		
		pb.environment().putAll(env);
		
		Process p = pb.start();
		
		ProcessMonitor mon = new ProcessMonitor(p);
		mon.start();
		
		return mon;
	}

	private static class ProcessMonitor extends Thread implements Input {
		
		private Process process;
		private InputStream stdOut;
		private InputStream stdErr;
		private OutputStream stdIn;
		
		public ProcessMonitor(Process process) {
			this.process = process;
			stdOut = process.getInputStream();
			stdErr = process.getErrorStream();
			stdIn = process.getOutputStream();
		}

		@Override
		public void run() {
			while(Shell.isAlive(process)) {
				if (!pump(stdOut, System.out) & !pump(stdErr, System.err)) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
			pump(stdOut, System.out);
			pump(stdErr, System.err);
		}

		@Override
		public Input write(byte[] raw) throws IOException {
			if (!Shell.isAlive(process)) {
				throw new IOException("Process has terminated");				
			}
			else {
				stdIn.write(raw);
				stdIn.flush();
			}
			return this;
		}

		@Override
		public Input write(String text) throws IOException {
			if (!Shell.isAlive(process)) {
				throw new IOException("Process has terminated");				
			}
			else {
				stdIn.write(text.getBytes());
				stdIn.flush();
			}
			return this;
		}

		@Override
		public Input writeln(String text) throws IOException {
			if (!Shell.isAlive(process)) {
				throw new IOException("Process has terminated");				
			}
			else {
				stdIn.write((text + "\n").getBytes());
				stdIn.flush();
			}
			return this;
		}

		@Override
		public void done() throws IOException, InterruptedException {
			stdIn.close();
			while(Shell.isAlive(process)) {
				if (!pump(stdOut, System.out) & !pump(stdErr, System.err)) {
					Thread.sleep(100);
				}
			}
			pump(stdOut, System.out);
			pump(stdErr, System.err);
			int exitCode = process.exitValue();
			if (exitCode != 0) {
				throw new NonZeroExitCodeException("Exit code " + exitCode);
			}			
		}
	}
	
	private static boolean isAlive(Process p) {
		try {
			p.exitValue();		
			return false;
		} catch (IllegalThreadStateException e) {
			return true;
		}
	}

	private static boolean pump(InputStream is, OutputStream os) {
		try {
			if (is.read(new byte[0]) < 0) {
				// EOF
				return false;
			}
			else {
				int avail = is.available();
				if (avail > 0) {
					byte[] buffer = new byte[avail];
					int n = is.read(buffer);
					if (n > 0) {
						os.write(buffer, 0, n);
						return true;
					}
				}
			}
		} catch (IOException e) {
			// ignore
		}
		return false;
	}	

	public interface Input {
		public Input write(byte[] raw) throws IOException;
		public Input write(String text) throws IOException;
		public Input writeln(String text) throws IOException;
		public void done() throws IOException, InterruptedException;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		get().exec("echo", "ABC", "123");
		get().exec(new File(System.getProperty("user.home")), "dir");

		Input x = get().execWithInput(new File("."), "cmd", "/C", "cat > x.txt");
		x.writeln("This is cat");
		x.writeln("Another line");
		x.done();
	}
	
	public static class NonZeroExitCodeException extends IOException {

		private static final long serialVersionUID = 20121118L;

		private NonZeroExitCodeException() {
			super();
		}

		private NonZeroExitCodeException(String message, Throwable cause) {
			super(message, cause);
		}

		private NonZeroExitCodeException(String message) {
			super(message);
		}

		private NonZeroExitCodeException(Throwable cause) {
			super(cause);
		}
	}
}
