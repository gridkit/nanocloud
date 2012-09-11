package org.gridkit.zerormi;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureEx;

public class RmiEndPointAdapter implements RmiEndPoint {
	
	private RmiInvocationHandler channel;
	private AdvancedExecutorAdapter executor;
	
	public RmiEndPointAdapter(RmiInvocationHandler channel) {
		this.channel = channel;
		this.executor = new AdvancedExecutorAdapter();
	}

	@Override
	public boolean isRemoteProxy(Object proxy) {
		return channel.isRemoteProxy(proxy);
	}

	@Override
	public void exportObject(Class<?> facade, Object impl) {
		channel.exportObject(facade, impl);
	}

	@Override
	public void exportObject(Class<?>[] facade, Object impl) {
		channel.exportObject(facade, impl);
	}
	
	@Override
	public AdvancedExecutor asExecutor() {
		return executor;
	}	
	
	public void shutdown() {
		channel.destroy();
	}
	
	private class AdvancedExecutorAdapter implements AdvancedExecutor {

		private Method callMethod;
		private Method runMethod;
		
		public AdvancedExecutorAdapter() {
			try {
				callMethod = Callable.class.getMethod("call");
				runMethod = Runnable.class.getMethod("run");
			} catch (Exception e) {
				throw new Error("Impossible", e);
			}
		}
		
		@Override
		public void execute(Runnable task) {
			channel.invokeRemotely(task, runMethod);
		}

		@Override
		public FutureEx<Void> submit(Runnable task) {
			return channel.invokeRemotely(task, runMethod);
		}

		@Override
		public <V> FutureEx<V> submit(Callable<V> task) {
			return channel.invokeRemotely(task, callMethod);
		}

		@Override
		public void schedule(Runnable task, long delay, TimeUnit tu) {
			throw new UnsupportedOperationException();
		}
	}
}
