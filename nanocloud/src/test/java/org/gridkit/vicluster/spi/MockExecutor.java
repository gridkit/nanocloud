package org.gridkit.vicluster.spi;

import java.util.concurrent.Callable;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;

public class MockExecutor implements AdvancedExecutor.Component {

	private Object value;
	
	public MockExecutor(Object value) {
		this.value = value;
	}

	@Override
	public void execute(Runnable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FutureEx<Void> submit(Runnable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V> FutureEx<V> submit(Callable<V> task) {
		FutureBox<V> box = new FutureBox<V>();
		box.setData((V) value);
		return box;
	}

	@Override
	public void shutdown() {
	}
}
