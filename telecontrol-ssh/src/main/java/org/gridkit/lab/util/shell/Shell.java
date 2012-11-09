package org.gridkit.lab.util.shell;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Shell {

	public static int syncExec(String... command) throws IOException, InterruptedException {
		return syncExec(new File("."), command);
	}

	public static int syncExec(File path, String... command) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(command);
		if (!".".equals(path.getPath())) {
			pb.directory(path);
		}
		
		Process p = pb.start();
		InputStream stdOut = p.getInputStream();
		InputStream stdErr = p.getErrorStream();
		while(isAlive(p)) {
			if (!pump(stdOut, System.out) & !pump(stdErr, System.err)) {
				Thread.sleep(100);
			}
		}
		pump(stdOut, System.out);
		pump(stdErr, System.err);
		return p.exitValue();
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
	
	public static void main(String[] args) throws IOException, InterruptedException {
		syncExec("echo", "ABC", "123");
		syncExec(new File(System.getProperty("user.home")), "dir");
	}
}
