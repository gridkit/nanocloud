package org.gridkit.vicluster.spi;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.MassExec;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.VoidCallable;

public abstract class AbstractMultipleNodeExecutor implements ViExecutor {

	protected abstract AdvancedExecutor[] getExecutors();

	@Override
	public void execute(Runnable task) {
		for(AdvancedExecutor e: getExecutors()) {
			e.execute(task);
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void exec(Runnable task) {
		Future<?>[] futures = new Future[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(task);
		}
		MassExec.waitAll((List)Arrays.asList(futures));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exec(VoidCallable task) {
		Future<?>[] futures = new Future[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(new VoidCallableWrapper(task));
		}
		MassExec.waitAll((List)Arrays.asList(futures));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T exec(Callable<T> task) {
		Future<?>[] futures = new Future[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(task);
		}
		return (T) MassExec.waitAll((List)Arrays.asList(futures)).get(0);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public FutureEx<Void> submit(Runnable task) {
		FutureEx<Void>[] futures = new FutureEx[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(task);
		}
		return (FutureEx)MassExec.vectorFuture(Arrays.asList(futures));		
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public FutureEx<Void> submit(VoidCallable task) {
		FutureEx<Void>[] futures = new FutureEx[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(new VoidCallableWrapper(task));
		}
		return (FutureEx)MassExec.vectorFuture(Arrays.asList(futures));		
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> FutureEx<T> submit(Callable<T> task) {
		FutureEx<T>[] futures = new FutureEx[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(task);
		}
		final FutureBox<T> fb = new FutureBox<T>();		
		MassExec.vectorFuture(Arrays.asList(futures)).addListener(new Box<List<T>>() {
			@Override
			public void setData(List<T> data) {
				fb.setData(data.get(0));
			}
			@Override
			public void setError(Throwable e) {
				fb.setError(e);
			}			
		});
		return fb;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<T> massExec(Callable<? extends T> task) {
		Future<?>[] futures = new FutureEx[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(task);
		}
		return (List)MassExec.collectAll(Arrays.asList(futures));		
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<FutureEx<Void>> massSubmit(Runnable task) {
		FutureEx<Void>[] futures = new FutureEx[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(task);
		}
		return (List)Arrays.asList(futures);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<FutureEx<Void>> massSubmit(VoidCallable task) {
		FutureEx<Void>[] futures = new FutureEx[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = getExecutors()[i].submit(new VoidCallableWrapper(task));
		}
		return (List)Arrays.asList(futures);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<FutureEx<T>> massSubmit(Callable<? extends T> task) {
		FutureEx<T>[] futures = new FutureEx[getExecutors().length];
		for(int i = 0; i != futures.length; ++i) {
			futures[i] = (FutureEx<T>) getExecutors()[i].submit(task);
		}
		return (List)Arrays.asList(futures);
	}

	@Override
	public <T> FutureEx<List<T>> vectorSubmit(Callable<? extends T> task) {
		return MassExec.vectorFuture(massSubmit(task));
	}
	
	private final class VoidCallableWrapper implements Callable<Void> {
		private final VoidCallable task;

		private VoidCallableWrapper(VoidCallable task) {
			this.task = task;
		}

		@Override
		public Void call() throws Exception {
			task.call();
			return null;
		}
	}
}
