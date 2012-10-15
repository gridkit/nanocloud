package org.gridkit.util.concurrent;

public interface RunnableEx extends Runnable {

	/**
	 * This method will be called in case of task abort.
	 * Executer is providing guaranty that exactly one method, {@link #run()} or {@link #cancelled()} will be called on submitted task. 
	 */
	public void cancelled();
}
