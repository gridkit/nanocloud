package org.gridkit.vicluster;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.VoidCallable;

public class DummyViNode implements ViNode {
	
	private static ExecutorService EXECUTOR = Executors.newCachedThreadPool();

	private ViNodeConfig config = new ViNodeConfig();

	@Override
	public String getProp(String propName) {
		return config.getProp(propName);
	}

	public void setProp(String propName, String value) {
		config.setProp(propName, value);
	}

	public void setProps(Map<String, String> props) {
		config.setProps(props);
	}

	public void addStartupHook(String name, Runnable hook, boolean override) {
		config.addStartupHook(name, hook, override);
	}

	public void addShutdownHook(String name, Runnable hook, boolean override) {
		config.addShutdownHook(name, hook, override);
	}

	@Override
	public void suspend() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void touch() {
	}

	@Override
	public void exec(Runnable task) {
		task.run();		
	}

	@Override
	public void exec(VoidCallable task) {
		try {
			task.call();
		} catch (Exception e) {
			AnyThrow.throwUncheked(e);
		}		
	}

	@Override
	public <T> T exec(Callable<T> task) {
		try {
			return task.call();
		} catch (Exception e) {
			AnyThrow.throwUncheked(e);
			throw new Error("Unreachable");
		}		
	}

	@Override
	@SuppressWarnings("unchecked")
	public Future<Void> submit(Runnable task) {
		return (Future<Void>) EXECUTOR.submit(task);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Future<Void> submit(final VoidCallable task) {
		return (Future<Void>) EXECUTOR.submit(new Runnable(){
			public void run() {
				try {
					task.call();
				} catch (Exception e) {
					AnyThrow.throwUncheked(e);
				}
			}
		});
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return EXECUTOR.submit(task);
	}

	@Override
	public <T> List<T> massExec(Callable<? extends T> task) {
		return Collections.singletonList((T)exec(task));
	}

	@Override
	public List<Future<Void>> massSubmit(Runnable task) {
		return Collections.singletonList(submit(task));
	}

	@Override
	public List<Future<Void>> massSubmit(VoidCallable task) {
		return Collections.singletonList(submit(task));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		return (List)Collections.singletonList(submit(task));
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
