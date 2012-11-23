package org.gridkit.lab.util.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.lab.util.shell.Shell.Input;

public interface Prompt {
	
	public Prompt env(String var, String value);
	
	public Prompt out(OutputStream stdOut);

	public Prompt out(StringBuilder stdOut);

	public Prompt cd(String path) throws IOException;
	
	public Prompt cd(String path, boolean mkdirs) throws IOException;
	
	public Prompt waitTimeout(long to, TimeUnit tu);
	
	public String pwd();
	
	public List<String> list() throws IOException;

	public List<String> list(String path) throws IOException;
	
	public List<String> find(String pattern)  throws IOException;

	public List<String> find(String path, String pattern)  throws IOException;

	public Prompt mkdirs(String path) throws IOException;
	
	public Prompt rm(String path) throws IOException;

	public Prompt rm(Collection<String> paths) throws IOException;

	public Prompt rm(String path, boolean rf) throws IOException;

	public Prompt rm(Collection<String> paths, boolean rf) throws IOException;
	
	public Prompt backup(String file) throws IOException;
	
	public Prompt backup(String file, boolean remove) throws IOException;

	public Prompt wget(String url) throws IOException;
	
	public Prompt extract(String file) throws IOException;
	
	public Prompt writeTo(String path, String text) throws IOException;

	public Prompt writeTo(String path, byte[] data) throws IOException;

	public Prompt writeTo(String path, InputStream data) throws IOException;

	public Prompt writeTo(String path, String text, boolean append) throws IOException;
	
	public Prompt writeTo(String path, byte[] data, boolean append) throws IOException;
	
	public Prompt writeTo(String path, InputStream data, boolean append) throws IOException;
	
	public boolean exists(String path) throws IOException;

	public Prompt waitForMatch(String path, String pattern) throws TimeoutException, IOException;
	
	public Prompt exec(String... command) throws IOException, InterruptedException;

	public Prompt exec(OutputStream stdOut, String... command) throws IOException, InterruptedException;

	public Prompt exec(StringBuilder stdOut, String... command) throws IOException, InterruptedException;

	public Prompt execAt(String path, String... command) throws IOException, InterruptedException;

	public Prompt execAt(String path, OutputStream stdOut, String... command) throws IOException, InterruptedException;

	public Prompt execAt(String path, StringBuilder stdOut, String... command) throws IOException, InterruptedException;
	
	public Input execWithInput(String... command) throws IOException, InterruptedException;

	public Input execWithInput(OutputStream stdOut, String... command) throws IOException, InterruptedException;

	public Input execWithInput(StringBuilder stdOut, String... command) throws IOException, InterruptedException;

	public Input execWithInputAt(String path, String... command) throws IOException, InterruptedException;

	public Input execWithInputAt(String path, StringBuilder stdOut, String... command) throws IOException, InterruptedException;

	public Input execWithInputAt(String path, OutputStream stdOut, String... command) throws IOException, InterruptedException;
}
