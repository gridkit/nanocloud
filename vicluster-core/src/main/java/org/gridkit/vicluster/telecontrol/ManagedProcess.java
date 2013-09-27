package org.gridkit.vicluster.telecontrol;

import java.io.InputStream;
import java.io.OutputStream;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureEx;

public interface ManagedProcess {

	public void suspend();
	
	public void resume();
	
	public void destroy();
	
	public FutureEx<Integer> getExitCodeFuture();
	
	public AdvancedExecutor getExecutionService();
	
	public void bindStdIn(InputStream is);
	
	public void bindStdOut(OutputStream os);
	
	public void bindStdErr(OutputStream os);
	
}
