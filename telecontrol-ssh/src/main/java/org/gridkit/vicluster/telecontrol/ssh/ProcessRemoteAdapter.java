/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.Remote;

import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.StreamPipe;

@SuppressWarnings("serial")
public class ProcessRemoteAdapter extends Process implements Serializable {

	private final transient Process process;
	private final RemoteProcess proxy;

	private OutputStream stdIn;
	private transient InputStream stdOut;
	private transient InputStream stdErr;

	public ProcessRemoteAdapter(Process process, StreamCopyService streamCopyService) {
		this.process = process;
		this.proxy = new ProcessProxy(streamCopyService);
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
			proxy.setStdErrReceiver(new OutputStreamRemoteAdapter(pipe.getOutputStream()));
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
		try {
			proxy.destroy();
		}
		catch(IOException e) {
			// TODO logging
		}
	}

	public interface RemoteProcess extends Remote {
		
		public void setStdOutReceiver(OutputStream stream);
		
		public void setStdErrReceiver(OutputStream stream);

		public int waitFor() throws InterruptedException;

		public int exitValue();

		public void destroy() throws IOException;
	}

	private class ProcessProxy implements RemoteProcess {

	    private final StreamCopyService streamCopyService;

		public ProcessProxy(StreamCopyService streamCopyService) {
            this.streamCopyService = streamCopyService;
        }

        @Override
		public void setStdOutReceiver(OutputStream stream) {
            streamCopyService.link(process.getInputStream(), stream);
		}

		@Override
		public void setStdErrReceiver(OutputStream stream) {
		    streamCopyService.link(process.getErrorStream(), stream);
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
