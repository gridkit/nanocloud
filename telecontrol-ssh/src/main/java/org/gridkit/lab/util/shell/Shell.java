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
package org.gridkit.lab.util.shell;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class Shell {

	public static Prompt prompt() {
		return new SimpleShell();
	}

	private static final long ANCHOR = System.nanoTime();
	private static long now() {
		return System.nanoTime() - ANCHOR;
	}
	
	public static class SimpleShell implements Prompt, Serializable {

		private static final long serialVersionUID = 20121118L;
		
		private String baseDir = ".";
		private OutputStream stdOut = System.out;
		private long waitTimeoutNs = TimeUnit.MINUTES.toNanos(5);
		private Map<String, String> env = new HashMap<String, String>();

		@Override
		public SimpleShell env(String var, String value) {
			env.put(var, value);
			return this;
		}
		
		@Override
		public Prompt out(OutputStream stdOut) {
			this.stdOut = stdOut == null ? System.out : stdOut;
			return this;
		}

        @Override
        @SuppressWarnings("resource")
		public Prompt out(StringBuilder stdOut) {
			this.stdOut = stdOut == null ? System.out : new StringBufferOutputStream(stdOut);
			return this;
		}

		@Override
		public Prompt waitTimeout(long to, TimeUnit tu) {
			waitTimeoutNs = tu.toNanos(to);
			return this;
		}

		@Override
		public SimpleShell cd(String path) throws IOException {
			File nb = resolvePath(path).getCanonicalFile();
			if (nb.exists() && nb.isDirectory()) {
				baseDir = nb.getPath();
			}
			else {
				throw new FileNotFoundException("No such directory: " + nb.getPath());
			}
			return this;
		}

		@Override
		public SimpleShell cd(String path, boolean mkdirs) throws IOException {
			if (mkdirs) {
				mkdirs(path);
			}
			File nb = resolvePath(path).getCanonicalFile();
			if (nb.exists() && nb.isDirectory()) {
				baseDir = nb.getPath();
			}
			else {
				throw new FileNotFoundException("No such directory: " + nb.getPath());
			}
			return this;
		}

		@Override
		public SimpleShell mkdirs(String path) throws IOException {
			File target = resolvePath(path);
			if (target.isFile()) {
				throw new IOException("File already exists: " + target.getPath());
			}
			if (!target.isDirectory()) {
				if (!target.mkdirs()) {
					throw new IOException("Filed to create path: " + target.getPath());
				}
			}
			return this;
		}
		
		@Override
		public boolean exists(String path) throws IOException {			
			return resolvePath(path).exists();
		}
		
		@Override
		public String pwd() {
			return new File(baseDir).getPath();
		}

		@Override
		public List<String> list() throws IOException {
			return list(".");
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public List<String> list(String path) throws IOException {
			File f = resolvePath(path);
			String[] fl = f.list();
			return (List) (fl ==null ? Collections.emptyList() : Arrays.asList(fl));
		}

		@Override
		public List<String> find(String pattern) throws IOException {
			return filter(pattern, findFiles("."));
		}

		@Override
		public List<String> find(String path, String pattern) throws IOException {			
			return filter(pattern, findFiles(path));
		}

		private List<String> filter(String pattern, Iterator<String> files) {
			Pattern p = GlobHelper.translate(pattern, "/");
			List<String> result = new ArrayList<String>();
			while(files.hasNext()) {
				String file = files.next();
				if (GlobHelper.match(p, file, "/")) {
					result.add(file);
				}
			}
			return result;
		}

		private Iterator<String> findFiles(String path) throws IOException {
			return new FileFinder(resolvePath(path));
		}

		@Override
		public Prompt rm(Collection<String> paths) throws IOException {
			for(String f: paths) {
				rm(f);
			}
			return this;
		}

		@Override
		public Prompt rm(Collection<String> paths, boolean rf) throws IOException {
			for(String f: paths) {
				rm(f, rf);
			}
			return this;
		}

		@Override
		public Prompt waitForMatch(String path, String pattern) throws TimeoutException, IOException {
			File file = resolvePath(path);
			Pattern re = Pattern.compile(pattern);
			long deadline = now() + waitTimeoutNs;
			while(true) {
				if (deadline < now()) {
					throw new TimeoutException();
				}
				try {
					Tailer tail = new Tailer(file);
					try {
						while(true) {
							if (deadline < now()) {
								throw new TimeoutException();
							}
							String line  = tail.nextLine();							
							if (line == null) {
								String rm = tail.getRemainder();
								if (re.matcher(rm).find()) {
									return this;
								}
								
								try {
									Thread.sleep(300);
								} catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
							}
							else {
								if (re.matcher(line).find()) {
									return this;
								}								
							}
						}
					}
					finally {
						tail.close();
					}
				}
				catch(IOException e) {
					try {
						Thread.sleep(300);
					} catch (InterruptedException ee) {
						throw new RuntimeException(ee);
					}
					continue;
				}
			}
		}

		@Override
		public Prompt backup(String file) throws IOException {
			backup(file, false);
			return this;
		}

		@Override
		public Prompt backup(String file, boolean remove) throws IOException {
			File source = resolvePath(file);
			if (source.exists()) {
				String newName = file + new SimpleDateFormat(".yyyyMMdd.HHmmss").format(new Date());
				File dest = resolvePath(newName);
				if (remove) {
					source.renameTo(dest);
				} 
				else {
					copy(source, dest);
				}
			}
			return this;
		}

		private void copy(File source, File dest) throws IOException {
			if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
				try {
					exec(new StringBuilder(), "xcopy", source.getAbsolutePath(), dest.getAbsolutePath(), "/E", "/I", "/Q");
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
			else {
				// assume Unix
				try {
					exec("cp", "-r", source.getAbsolutePath(), dest.getAbsolutePath());
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
		}

		@Override
		public Prompt rm(String path) throws IOException {
			return rm(path, false);
		}

		@Override
		public Prompt rm(String path, boolean rf) throws IOException {
			File file = resolvePath(path);
			if (rf) {
				remove(file);
			}
			else {
				file.delete();
				if (file.exists()) {
					throw new IOException("Cannot remove: " + file.getPath());
				}
			}
			return this;
		}

		private void remove(File file) throws IOException {
			if (file.isDirectory()) {
				File[] list = file.listFiles();
				if (list != null) {
					for(File l: list) {
						remove(l);
					}
				}
				file.delete();
			}
			else {
				if (file.exists()) {
					file.delete();
				}
			}			
			if (file.exists()) {
				throw new IOException("Cannot delete: " + file.getPath());
			}
		}

		@Override
		public Prompt wget(String url) throws IOException {
			URL u = new URL(url);
			String name = last(u.getPath());
			FileOutputStream out = new FileOutputStream(resolvePath(name)); 
			InputStream is = u.openStream();
			ArchHelper.copy(is, out);
			out.close();
			is.close();			
			return this;
		}

		@Override
		public Prompt extract(String path) throws IOException {
			File file = resolvePath(path);
			if (!file.isFile()) {
				throw new FileNotFoundException(file.getPath());
			}
			if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
				ArchHelper.uncompressZip(file, resolvePath("."));
			}
			else if (file.getName().endsWith(".tar.gz")) {
				String ungz = file.getName().substring(0, file.getName().length() - 3);
				File dest = new File(file.getParent(), ungz);
				ArchHelper.uncompressGz(file, dest);
				if (ungz.endsWith(".tar")) {
					untar(dest);
				}
			}
			else if (file.getName().endsWith(".tar")) {
				untar(file);
			}
			else {
				throw new IOException("Unsupported archive type: " + path);
			}
			return this;
		}

		private void untar(File file) throws IOException {
			try {
				System.out.println("tar -xf " + file.getAbsolutePath());
				exec("tar", "-xf", file.getAbsolutePath());
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}

		private String last(String path) {
			int n = path.lastIndexOf('/');
			return n < 0 ? path : path.substring(n + 1);
		}

		@Override
		public Prompt writeTo(String path, String text) throws IOException {
			return writeTo(path, text.getBytes());
		}

		@Override
		public Prompt writeTo(String path, byte[] data) throws IOException {
			return writeTo(path, new ByteArrayInputStream(data));
		}

		@Override
		public Prompt writeTo(String path, InputStream data) throws IOException {
			return writeTo(path, data, false);
		}

		@Override
		public Prompt writeTo(String path, String text, boolean append) throws IOException {
			return writeTo(path, text.getBytes(), append);
		}

		@Override
		public Prompt writeTo(String path, byte[] data, boolean append) throws IOException {
			return writeTo(path, new ByteArrayInputStream(data), append);
		}

		@Override
		public Prompt writeTo(String path, InputStream data, boolean append) throws IOException {
			File file = resolvePath(path);
			FileOutputStream fos = new FileOutputStream(file, append);
			ArchHelper.copy(data, fos);
			fos.close();
			return this;
		}

		@Override
		public SimpleShell exec(String... command) throws IOException, InterruptedException {
			return execAt(".", command);
		}

		@Override
		public SimpleShell execAt(String path, String... command) throws IOException, InterruptedException {
			execAt(path, stdOut, command);
			return this;
		}
		
		@Override
		public Prompt exec(OutputStream stdOut, String... command) throws IOException, InterruptedException {
			execAt(".", stdOut, command);
			return this;
		}

		@Override
		public Prompt exec(StringBuilder stdOut, String... command) throws IOException, InterruptedException {
			exec(new StringBufferOutputStream(stdOut), command);
			return this;
		}

		@Override
		public Prompt execAt(String path, OutputStream stdOut, String... command) throws IOException, InterruptedException {
			File dir = resolvePath(path);
			int ex = syncProcExec(dir, stdOut, command);
			if (ex != 0) {
				throw new NonZeroExitCodeException("Exit code: " + ex); 
			}
			return this;
		}

		@Override
		public Prompt execAt(String path, StringBuilder stdOut, String... command) throws IOException, InterruptedException {
			execAt(path, new StringBufferOutputStream(stdOut), command);
			return this;
		}

		@Override
		public ChildProcess execInteractive(String... command) throws IOException, InterruptedException {
			return execInteractiveAt(".", stdOut, command);
		}

		@Override
		public ChildProcess execInteractive(OutputStream stdOut, String... command) throws IOException, InterruptedException {
			return execInteractiveAt(".", stdOut, command);
		}

		@Override
		public ChildProcess execInteractive(StringBuilder stdOut, String... command) throws IOException, InterruptedException {
			return execInteractiveAt(".", stdOut, command);
		}

		@Override
		public ChildProcess execInteractiveAt(String path, String... command) throws IOException, InterruptedException {
			return execInteractiveAt(path, stdOut, command);
		}

		@Override
		public ChildProcess execInteractiveAt(String path, StringBuilder stdOut, String... command) throws IOException, InterruptedException {
			return execInteractiveAt(path, new StringBufferOutputStream(stdOut), command);
		}

		@Override
		public ChildProcess execInteractiveAt(String path, OutputStream stdOut, String... command) throws IOException, InterruptedException {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(resolvePath(path));
			
			pb.environment().putAll(env);
			
			Process p = pb.start();
			
			ProcessMonitor mon = new ProcessMonitor(p, stdOut, System.err);
			mon.start();
			
			return mon;
		}

		private File resolvePath(String path) throws IOException {
			if (path.equals("{temp}")) {
				path = getTempDir().getPath();
			}
			else if (path.startsWith("{temp}/")) {
				path = new File(getTempDir(), path.substring("{temp}/".length())).getPath();
			}
			else if (path.equals("~")) {
				path = System.getProperty("user.home");
			}
			else if (path.startsWith("~/")) {
				String home = System.getProperty("user.home");
				path = new File(new File(home), path.substring(2)).getPath();
			}
			File p = new File(path);
			if (p.isAbsolute()) {
				return p;
			}
			else {
				File rpath = new File(new File(baseDir), path);
				return rpath;
			}
		}
		
		private File getTempDir() throws IOException {
			File file = File.createTempFile("vigrid", ".tmp");
			return file.getParentFile();
		}

		int syncProcExec(String... command) throws IOException, InterruptedException {
			return syncProcExec(new File(baseDir), System.out, command);
		}

		int syncProcExec(File path, OutputStream outSink, String... command) throws IOException, InterruptedException {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(path);

			pb.environment().putAll(env);
			
			Process p = pb.start();		
			InputStream stdOut = p.getInputStream();
			InputStream stdErr = p.getErrorStream();
			p.getOutputStream().close();
			while(isAlive(p)) {
				if (!pump(stdOut, outSink) & !pump(stdErr, System.err)) {
					Thread.sleep(100);
				}
			}
			pump(stdOut, outSink);
			pump(stdErr, System.err);
			outSink.flush();
			return p.exitValue();
		}

		private static class ProcessMonitor extends Thread implements ChildProcess {
			
			private Process process;
			private InputStream stdOut;
			private InputStream stdErr;
			private OutputStream stdIn;
			private OutputStream outSink;
			private OutputStream errSink;
			
			public ProcessMonitor(Process process, OutputStream outSink, OutputStream errSink) {
				this.process = process;
				stdOut = process.getInputStream();
				stdErr = process.getErrorStream();
				stdIn = process.getOutputStream();
				this.outSink = outSink;
				this.errSink = errSink;
			}

			@Override
			public void run() {
				while(SimpleShell.isAlive(process)) {
					if (!pump(stdOut, outSink) & !pump(stdErr, errSink)) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							return;
						}
					}
				}
				pump(stdOut, outSink);
				pump(stdErr, errSink);
				try {
					outSink.flush();
				} catch (IOException e) {
					// do nothing
				}
				try {
					errSink.flush();
				} catch (IOException e) {
					// do nothing
				}
			}

			@Override
			public ChildProcess write(byte[] raw) throws IOException {
				if (!SimpleShell.isAlive(process)) {
					throw new IOException("Process has terminated");				
				}
				else {
					stdIn.write(raw);
					stdIn.flush();
				}
				return this;
			}

			@Override
			public ChildProcess write(String text) throws IOException {
				if (!SimpleShell.isAlive(process)) {
					throw new IOException("Process has terminated");				
				}
				else {
					stdIn.write(text.getBytes());
					stdIn.flush();
				}
				return this;
			}

			@Override
			public ChildProcess writeln(String text) throws IOException {
				if (!SimpleShell.isAlive(process)) {
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
				while(SimpleShell.isAlive(process)) {
					if (!pump(stdOut, outSink) & !pump(stdErr, errSink)) {
						Thread.sleep(100);
					}
				}
				pump(stdOut, outSink);
				pump(stdErr, errSink);
				int exitCode = process.exitValue();
				if (exitCode != 0) {
					throw new NonZeroExitCodeException("Exit code " + exitCode);
				}			
			}

			@Override
			public void done(long timeout, TimeUnit tu) throws IOException, InterruptedException, TimeoutException {
				long deadline = System.nanoTime() + tu.toNanos(timeout);
				stdIn.close();
				while(SimpleShell.isAlive(process)) {
					if (!pump(stdOut, outSink) & !pump(stdErr, errSink)) {
						if (deadline - System.nanoTime() > 0) {
							Thread.sleep(100);
						}
					}
					if (deadline - System.nanoTime() <= 0) {
						process.destroy();
						pump(stdOut, outSink);
						pump(stdErr, errSink);
						stdOut.close();
						stdErr.close();
						throw new TimeoutException();
					}
				}
				pump(stdOut, outSink);
				pump(stdErr, errSink);
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

		private static class FileFinder implements Iterator<String> {
			
			private File root;
			private List<File> stack = new ArrayList<File>();
			private List<String> files = new ArrayList<String>();
					
			public FileFinder(File file) {
				root = file;
				stack.add(file);
			}

			@Override
			public boolean hasNext() {
				if (files.isEmpty()) {
					while(!stack.isEmpty()) {
						File top = stack.remove(stack.size() - 1);
						File[] list = top.listFiles();
						if (list != null) {
							for(File f: list) {
								files.add(getRelPath(root, f));
								if (f.isDirectory()) {
									stack.add(f);
								}
							}
						}
					}
				}
				return !files.isEmpty();
			}

			private String getRelPath(File r, File f) {
				if (f.getParentFile().equals(r)) {
					return f.getName();
				}
				else {
					return getRelPath(r, f.getParentFile()) + "/" + f.getName();
				}
			}

			@Override
			public String next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				return files.remove(0);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
		
		private static class StringBufferOutputStream extends OutputStream {
			
			private final StringBuilder buf;

			private StringBufferOutputStream(StringBuilder buf) {
				super();
				this.buf = buf;
			}

			@Override
			public void write(int b) throws IOException {
				buf.append((char)b);
			}
		}
		
		public static void main(String[] args) throws IOException, InterruptedException {
			prompt().exec("echo", "ABC", "123");
			prompt().execAt("~", "dir");
			StringBuilder list = new StringBuilder();
			prompt()
				.cd("~").exec(list, "dir")
				.cd("C:\\").cd("WarZone").exec(list, "dir")
				.out(list)
				.exec("dir");
			System.err.println(list);
			
			ChildProcess x = prompt().execInteractiveAt(".", "cmd", "/C", "cat");
			x.writeln("This is cat");
			x.writeln("Another line");
			x.done();
			
			prompt().cd("~/test-dir", true).exec("pwd");
			prompt().cd("{temp}/test-dir", true).exec("pwd");
			
			prompt().cd("~").backup("test-dir");
		}
	}	
	
	public interface ChildProcess {
		public ChildProcess write(byte[] raw) throws IOException;
		public ChildProcess write(String text) throws IOException;
		public ChildProcess writeln(String text) throws IOException;
		public void done() throws IOException, InterruptedException;
		public void done(long timeout, TimeUnit tu) throws IOException, InterruptedException, TimeoutException;
	}
	
	public static class NonZeroExitCodeException extends IOException {

		private static final long serialVersionUID = 20121118L;

		NonZeroExitCodeException() {
			super();
		}

		NonZeroExitCodeException(String message, Throwable cause) {
			super(message, cause);
		}

		NonZeroExitCodeException(String message) {
			super(message);
		}

		NonZeroExitCodeException(Throwable cause) {
			super(cause);
		}
	}	
}
