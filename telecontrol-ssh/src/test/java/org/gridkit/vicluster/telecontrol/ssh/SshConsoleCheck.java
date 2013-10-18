package org.gridkit.vicluster.telecontrol.ssh;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.junit.Test;

public class SshConsoleCheck {

	@Test
	public void test_remote_echo() throws JSchException, InterruptedException, ExecutionException {
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("root");
		sshFactory.setPassword("reverse");
		
		Session session = sshFactory.getSession("cbox1", null);
		
		SshHostControlConsole console= new SshHostControlConsole(session, "~/.nanocloud/cache");

		final FutureBox<Void> exit = new FutureBox<Void>();
		
		console.startProcess("~/target/", new String[]{"pwd"}, null, new ProcessHandler() {
			
			@Override
			public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
				System.out.println("Process started");
				try {
//					stdIn.close();
					BackgroundStreamDumper.link(stdOut, System.out, false);
					BackgroundStreamDumper.link(stdErr, System.err, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void finished(int exitCode) {
				System.out.println("Exit code: " + exitCode);
				exit.setData(null);
			}
		});		
		
		exit.get();
	}

	
}
