package org.gridkit.vicluster.isolate.instrumentation;

import java.util.concurrent.ExecutionException;

public interface ExecutionHook {
	
	public void handle(HookContext hook);
	
	public interface HookContext {
		
		public HookType getHookType();
		
		public Class<?> getHostClass();

		public Object getReflectionObject();
		
		
		/**
		 * @return array of arguments, array could be modified to modify arguments passing through.
		 */
		public Object[] getArguments();

		/**
		 * @return result of call being hooked
		 * @throws ExecutionException
		 */
		public Object call() throws ExecutionException;
		
		/**
		 * Sets results and suppress invocation of original code
		 */
		public void setResult(Object result);

		/**
		 * Sets error and suppress invocation of original code
		 */
		public void setError(Throwable e);
		
	}

}
