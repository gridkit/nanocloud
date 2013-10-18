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
import java.util.Map;

import org.gridkit.internal.com.jcraft.jsch.ChannelExec;
import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.vicluster.telecontrol.ExecCommand;


/**
 * An implementation of {@link Process} representing process executed via SSH facility.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteSshProcess extends Process {

	private OutputStream stdin;
	private InputStream stdout;
	private InputStream stderr;
	
	private ChannelExec channel;
	
	public RemoteSshProcess(Session session, ExecCommand command) throws JSchException, IOException {
		
		channel = (ChannelExec) session.openChannel("exec");
		String cmd = command.getCommand();
		if (command.getWorkDir() != null) {
			cmd = "cd \"" + command.getWorkDir() + "\" && " + cmd;
		}
		channel.setCommand(cmd);
		stdin = channel.getOutputStream();
		stderr = channel.getErrStream();
		stdout = channel.getInputStream();
		
		for(Map.Entry<String, String> entry: command.getEviroment().entrySet()) {
			channel.setEnv(entry.getKey(), entry.getValue());
		}
		
		channel.connect();
	}

	@Override
	public int exitValue() {
		if (channel.isClosed()) {
			return channel.getExitStatus();
		}
		throw new IllegalStateException("Running");
	}
	
	@Override
	public OutputStream getOutputStream() {
		return stdin;
	}
	
	@Override
	public InputStream getInputStream() {
		return stdout;
	}

	@Override
	public InputStream getErrorStream() {
		return stderr;
	}

	@Override
	public int waitFor() throws InterruptedException {
		// how ??
		while(true) {
			if (channel.isClosed()) {
				break;
			}
			Thread.sleep(100);
		}
		return channel.getExitStatus();
	}
	
	@Override
	public void destroy() {
		if (!channel.isClosed()) {
			try {
				channel.sendSignal("KILL");
			} catch (Exception e) {
				// ignore
			}
		}
	}
}
