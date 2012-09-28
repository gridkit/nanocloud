package org.gridkit.vicluster.telecontrol.ssh;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.Remote;

import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;

@SuppressWarnings("serial")
public class ProcessRemoteAdapter extends Process implements Serializable {
	
	private final transient Process process;
	private final RemoteProcess proxy;
	
	private OutputStream stdIn;
	private transient InputStream stdOut;
	private transient InputStream stdErr;
	
	public ProcessRemoteAdapter(Process process) {
		this.process = process;
		this.proxy = new ProcessProxy();
		this.stdIn = new OutputStreamRemoteAdapter(process.getOutputStream());
	}
	
	@Override
	public OutputStream getOutputStream() {
		return stdIn;
	}

	@Override
	public synchronized InputStream getInputStream() {
		if (stdOut == null) {
			StreamPipe pipe = new StreamPipe(16 << 10);
			proxy.setStdOutReceiver(new OutputStreamRemoteAdapter(pipe.getOutputStream()));
			stdOut = pipe.getInputStream();
		}
		return stdOut;
	}

	@Override
	public synchronized InputStream getErrorStream() {
		if (stdErr == null) {
			StreamPipe pipe = new StreamPipe(16 << 10);
			proxy.setStdOutReceiver(new OutputStreamRemoteAdapter(pipe.getOutputStream()));
			stdErr = pipe.getInputStream();
		}
		return stdErr;
	}

	@Override
	public int waitFor() throws InterruptedException {
		return proxy.waitFor();
	}

	@Override
	public int exitValue() {
		return proxy.exitValue();
	}

	@Override
	public void destroy() {
		proxy.destroy();
	}

	public interface RemoteProcess extends Remote {
		
		public void setStdOutReceiver(OutputStream stream);
		
		public void setStdErrReceiver(OutputStream stream);

		public int waitFor() throws InterruptedException;

		public int exitValue();

		public void destroy();
	}
	
	private class ProcessProxy implements RemoteProcess {

		@Override
		public void setStdOutReceiver(OutputStream stream) {
			BackgroundStreamDumper.link(process.getInputStream(), stream);
		}

		@Override
		public void setStdErrReceiver(OutputStream stream) {
			BackgroundStreamDumper.link(process.getErrorStream(), stream);
		}

		@Override
		public int waitFor() throws InterruptedException {
			return process.waitFor();
		}

		@Override
		public int exitValue() {
			return process.exitValue();
		}

		@Override
		public void destroy() {
			process.destroy();
		}
	}
}
