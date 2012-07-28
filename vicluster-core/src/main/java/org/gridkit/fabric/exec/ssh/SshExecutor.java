/**
 * Copyright 2008-2009 Grid Dynamics Consulting Services, Inc.
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
package org.gridkit.fabric.exec.ssh;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SshExecutor {
	
	private final Logger logger = Logger.getLogger(SshExecutor.class.getName());
	
	private final String login;
	private final String password;
	private String remoteHome;
	
	private final UserInfo userInfo = new UserInfo() {

		public String getPassphrase() {
			return password;
		}

		public String getPassword() {
			return password;
		}

		public boolean promptPassphrase(String message) {
			logger.fine("SSH:promptPassphrase: " + message);
			return password != null;
		}

		public boolean promptPassword(String message) {
			logger.fine("SSH:promptPassword: " + message);
			return password != null;
		}

		public boolean promptYesNo(String message) {
			logger.fine("SSH:promptYesNo: " + message);
			return true;
		}

		public void showMessage(String message) {
			logger.fine("SSH:showMessage: " + message);
		}
	};
	
	private JSch jsch = new JSch();
	private Executor threadPool = Executors.newCachedThreadPool();
	private ConcurrentMap<String, Queue<Session>> sessionPool = new ConcurrentHashMap<String, Queue<Session>>();
	
	public SshExecutor(String login, String password, String keyFile) {
		this.login = login;
		this.password = password;
		if (keyFile != null) {
			try {
				jsch.addIdentity(keyFile);
			} catch (JSchException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void setRemoteHome(String home) {
		this.remoteHome = home;
	}

	public Future<ExecutionResult> execute(final String host, final String command) {
		return execute(host, command, null);
	}

	public Future<ExecutionResult> execute(final String host, final String command, final Callback callback) {
		Callable<ExecutionResult> task = new Callable<ExecutionResult>() {
			public ExecutionResult call() throws Exception {
				ExecutionResult result = runTask(host.toLowerCase(), command);
				if (callback != null) {
					callback.finished(result.exitCode, result.stdout, result.stderr);
				}
				return result;
			}
		};
		
		FutureTask<ExecutionResult> future = new FutureTask<ExecutionResult>(task);
		threadPool.execute(future);
		
		return future;
	}
		
	private ExecutionResult runTask(String host, String command) {
		Queue<Session> sessions = sessionPool.get(host);
		if (sessions == null) {
			sessions = new ConcurrentLinkedQueue<Session>();
			sessionPool.putIfAbsent(host, sessions);
			sessions = sessionPool.get(host);
		}
		
		int exitCode = 0;
		ByteArrayOutputStream errStr = new ByteArrayOutputStream();
		ByteArrayOutputStream outStr = new ByteArrayOutputStream();
		try {
			Session session = sessions.poll();
			if (session == null) {
				session = createSession(host);
			}
			
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			if (remoteHome != null) {
				command = "cd " + remoteHome + ";" + command;
			}
			channel.setCommand(command);
			channel.setInputStream(null);
			
			((ChannelExec)channel).setErrStream(errStr);
			
			InputStream in=channel.getInputStream();
			long startTime = System.nanoTime();
			channel.connect();
			
			byte[] tmp=new byte[1024];
			while(true){
			    while(true){
			    	int i=in.read(tmp, 0, 1024);
			    	if(i<0) {
			    		break;
			    	}
			    	outStr.write(tmp, 0, i);
			    }
			    if(channel.isClosed()){
			    	exitCode = channel.getExitStatus();
			    	break;
			    }
			}
			channel.disconnect();
			long endTime = System.nanoTime();
			sessions.add(session);

			String outText = new String(outStr.toByteArray());
			String errText = new String(errStr.toByteArray());
			
			return new ExecutionResult(outText, errText, exitCode, TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
			
		} catch (Exception e) {
			logger.severe("SSH fauilure [" + host + "]" + e);
			logger.severe("SSH [" + host + "] command: " + command);
			logger.severe("SSH [" + host + "] stdout\n" + new String(outStr.toByteArray()));
			logger.severe("SSH [" + host + "] stderr\n" + new String(errStr.toByteArray()));
			throw new RuntimeException(e);
		}	
	}
	
	private Session createSession(String host) throws JSchException {
		
		Session session = jsch.getSession(login, host);
//		if (password != null) {
			session.setUserInfo(userInfo);
//		}
		session.connect();
		
		return session;
	}

	public static interface Callback {
		public void finished(int exitCode, String stdOut, String stdErr); 
	}
	
	public static class ExecutionResult {
		public String stdout;
		public String stderr;
		public int exitCode;
		public long executionTime;
		
		public ExecutionResult(String stdout, String stderr, int exitCode, long executionTime) {
			this.stdout = stdout;
			this.stderr = stderr;
			this.exitCode = exitCode;
			this.executionTime = executionTime;
		}
	}

}
