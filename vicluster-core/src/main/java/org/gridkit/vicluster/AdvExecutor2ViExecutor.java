package org.gridkit.vicluster;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.AdvancedExecutor;

/**
 * A shim between two executor classes.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class AdvExecutor2ViExecutor implements ViExecutor {
	
    public static <T> T exec(AdvancedExecutor exec, Callable<T> task) {
        return new AdvExecutor2ViExecutor(exec).exec(task);
    }

    public static void exec(AdvancedExecutor exec, Runnable task) {
        new AdvExecutor2ViExecutor(exec).exec(task);
    }
    
	private final AdvancedExecutor advExec;

	public AdvExecutor2ViExecutor(AdvancedExecutor advExec) {
		this.advExec = advExec;
	}

	protected AdvancedExecutor getExecutor() {
		return advExec;
	}
	
	@Override
	public void exec(Runnable task) {
		try {
			submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
		}
	}

	@Override
	public void exec(VoidCallable task) {
		try {
			submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
		}
	}

	@Override
	public <T> T exec(Callable<T> task) {
		try {
			return submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
			throw new Error("Unreachable");			
		}
	}

	@Override
	public Future<Void> submit(Runnable task) {
		return (Future<Void>) getExecutor().submit(task);
	}

	@Override
	public Future<Void> submit(VoidCallable task) {
		return getExecutor().submit(new VoidCallable.VoidCallableWrapper(task));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return getExecutor().submit(task);
	}

	@Override
	public <T> List<T> massExec(Callable<? extends T> task) {
		return MassExec.singleNodeMassExec(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(Runnable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(VoidCallable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}
}
