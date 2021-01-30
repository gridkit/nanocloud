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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.internal.com.jcraft.jsch.ChannelSftp;
import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.internal.com.jcraft.jsch.SftpException;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SftFileCache implements RemoteFileCache {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(SftFileCache.class);
	
	private final Session session;
	private final String jarCachePath;
	private final boolean useRelativePaths;
	private String absoluteCachePath;
	private BlockingQueue<ChannelSftp> channels = new LinkedBlockingQueue<ChannelSftp>();
	private ExecutorService executor;

	private Map<String, String> fileMapping = new ConcurrentHashMap<String, String>();

	public SftFileCache(Session session, String cachePath, boolean useRelativePaths, int concurency) throws JSchException, InterruptedException, SftpException {
		this.session = session;
		this.jarCachePath = cachePath;
		this.useRelativePaths = useRelativePaths;
		if (concurency < 0) {
			throw new IllegalArgumentException("concurency should be positive");
		}
		for(int i = 0; i != concurency; ++i) {
			ChannelSftp sftp = (ChannelSftp)session.openChannel("sftp");
			channels.add(sftp);
		}
		if (concurency == 1) {
			executor = new SameThreadExecutor();
		}
		else {
			final String host = this.session.getUserName() + "@" + this.session.getHost();
			ThreadFactory tf = new ThreadFactory() {
				
				int counter;
				
				@Override
				public synchronized Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setDaemon(true);
					t.setName("SftpWorker-" + host + "-" + (counter++));
					return t;
				}
			};
			executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 100, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), tf);
		}
		
		mkdirs();
		prime();
	}

	private void mkdirs() throws InterruptedException, SftpException, JSchException {
		ChannelSftp sftp = getSftp();
		sftp.connect();
		sftpMkdirs(sftp, jarCachePath);
		sftp.cd(jarCachePath);
		absoluteCachePath = sftp.pwd();
		release(sftp);
	}
	
	private void prime() throws JSchException, SftpException {
		List<ChannelSftp> all = new ArrayList<ChannelSftp>();
		channels.drainTo(all);
		for(ChannelSftp sftp: all) {
			if (!sftp.isConnected()) {
				sftp.connect();
				sftp.cd(absoluteCachePath);
			}
			release(sftp);
		}
	}

	private ChannelSftp getSftp() throws InterruptedException {
		return channels.take();
	}

	private void release(ChannelSftp sftp) {
		channels.add(sftp);
	}

	@Override
	public String upload(FileBlob blob) {
		try {
			if (fileMapping.containsKey(blob.getContentHash())) {
				return fileMapping.get(blob.getContentHash());
			}
			ChannelSftp sftp = getSftp();
			try {
				return upload(sftp, blob);
			}
			finally {
				release(sftp);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted");
		}
	}

	@Override
	public List<String> upload(List<? extends FileBlob> blobs) {
		
		final String[] result = new String[blobs.size()];
		List<Future<?>> futures = new ArrayList<Future<?>>();
		
		try {
			for(int i = 0; i != blobs.size(); ++i) {
				final int n = i;
				final FileBlob blob = blobs.get(i);
				if (fileMapping.containsKey(blob.getContentHash())) {
					result[i] = fileMapping.get(blob.getContentHash());
					continue;
				}
				final ChannelSftp sftp = getSftp();
				futures.add(executor.submit(new Runnable() {
					
					@Override
					public void run() {
						try {
							result[n] = upload(sftp, blob);
						}
						finally {
							release(sftp);
						}
					}
				}));
			}
			
			for(Future<?> f: futures) {
				try {
					f.get();
				} catch (ExecutionException e) {
					throw new RuntimeException(e.getCause());
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted");
		}
		
		return Arrays.asList(result);
	}

	private String upload(ChannelSftp sftp, FileBlob blob) {
	    String blobPath = blob.getContentHash() + "/" + blob.getFileName();
		String rname = blobPath;
		try {
			sftpMkdirs(sftp, blob.getContentHash());
		} catch (SftpException e) {
			new RuntimeException("SFT error: " + e.getMessage());
		}
		int tries = 2;
		while(tries > 0) {
			--tries;
			try {
				if (!exists(sftp, rname)) {
					LOGGER.info("Uploading: " + session.getHost() + ":" + rname + " " + blob.size() + " bytes");
					sftp.put(blob.getContent(), rname);
				}
				else {
					LOGGER.debug("Already exists: " + session.getHost() + ":" + rname + " " + blob.size() + " bytes");
				}
				break;
			}
			catch(SftpException e) {
				if (tries > 0) {
					LOGGER.warn("upload \"" + rname + "\" failed: " + e.toString());
				}
				else {
					new RuntimeException("SFT error: " + e.getMessage());
				}
			}
		}
		if (useRelativePaths) {
		    if (jarCachePath.length() == 0 || jarCachePath.endsWith("/")) {
		        rname = jarCachePath + blobPath;
		    }
		    else {
		        rname = jarCachePath + "/" + blobPath;
		    }
		}
		fileMapping.put(blob.getContentHash(), rname);
		return rname;
	}
	

	private static void sftpMkdirs(ChannelSftp sftp, String path) throws SftpException {
		if (path.lastIndexOf('/') > 0) {
			String parPath = path.substring(0, path.lastIndexOf('/'));
			sftpMkdirs(sftp, parPath);
		}
		int tries = 2;
		while(tries > 0) {
			--tries;
			try {
				if (!exists(sftp, path)) {
					sftp.mkdir(path);			
				}
				return;
			}
			catch(SftpException e) {
				if (tries > 0) {
					LOGGER.warn("mkdir has failed: " + e.toString());
				}
				else {
					throw e;
				}
			}
		}
	}

	public void close() {
		// TODO close channels
		executor.shutdown();
	}
	
	private static boolean exists(ChannelSftp sftp, String path) {
		try {
			return sftp.stat(path) != null;
		} catch (SftpException e) {
			return false;
		}
	}
	
	private static class SameThreadExecutor implements ExecutorService {

		@Override
		public void execute(Runnable command) {
			command.run();
		}

		@Override
		public void shutdown() {
		}

		@Override
		public List<Runnable> shutdownNow() {
			return Collections.emptyList();
		}

		@Override
		public boolean isShutdown() {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return false;
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			FutureTask<T> f = new FutureTask<T>(task);
			f.run();
			return f;
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			FutureTask<T> f = new FutureTask<T>(task, result);
			f.run();
			return f;
		}

		@Override
		public Future<?> submit(Runnable task) {
			FutureTask<?> f = new FutureTask<Void>(task, null);
			f.run();
			return f;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
			return null;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException,	ExecutionException, TimeoutException {
			throw new UnsupportedOperationException();
		}		
	}
}
