package org.gridkit.vicluster.spi;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.MassExec;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.VoidCallable;

public abstract class AbstractSingleNodeExecutor implements ViExecutor {

	protected abstract AdvancedExecutor getExecutor();

	@Override
	public void execute(Runnable task) {
		getExecutor().execute(task);
	}

	@Override
	public void exec(Runnable task) {
		try {
			getExecutor().submit(task).get();
		}
		catch(Exception e) {
			if (e instanceof ExecutionException) {
				AnyThrow.throwUncheked(((ExecutionException)e).getCause());
			}
			else {
				AnyThrow.throwUncheked(e);
			}
		}		
	}

	@Override
	public void exec(final VoidCallable task) {
		try {
			getExecutor().submit(new VoidCallableWrapper(task)).get();
		}
		catch(Exception e) {
			if (e instanceof ExecutionException) {
				AnyThrow.throwUncheked(((ExecutionException)e).getCause());
			}
			else {
				AnyThrow.throwUncheked(e);
			}
		}		
	}

	@Override
	public <T> T exec(Callable<T> task) {
		try {
			return getExecutor().submit(task).get();
		}
		catch(Exception e) {
			if (e instanceof ExecutionException) {
				AnyThrow.throwUncheked(((ExecutionException)e).getCause());
				throw new Error("Unreacable");
			}
			else {
				AnyThrow.throwUncheked(e);
				throw new Error("Unreacable");
			}
		}		
	}

	@Override
	public FutureEx<Void> submit(Runnable task) {		
		return getExecutor().submit(task);
	}

	@Override
	public FutureEx<Void> submit(VoidCallable task) {
		return getExecutor().submit(new VoidCallableWrapper(task));
	}

	@Override
	public <T> FutureEx<T> submit(Callable<T> task) {
		return getExecutor().submit(task);
	}

	@Override
	public <T> List<T> massExec(Callable<? extends T> task) {
		return MassExec.singleNodeMassExec(this, task);
	}

	@Override
	public List<FutureEx<Void>> massSubmit(Runnable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public List<FutureEx<Void>> massSubmit(VoidCallable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public <T> List<FutureEx<T>> massSubmit(Callable<? extends T> task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> FutureEx<List<T>> vectorSubmit(Callable<? extends T> task) {
		return MassExec.vectorFuture((List)Collections.singletonList(getExecutor().submit(task)));
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

	private static class AnyThrow {

	    public static void throwUncheked(Throwable e) {
	        AnyThrow.<RuntimeException>throwAny(e);
	    }
	   
	    @SuppressWarnings("unchecked")
	    private static <E extends Throwable> void throwAny(Throwable e) throws E {
	        throw (E)e;
	    }
	}	
}
