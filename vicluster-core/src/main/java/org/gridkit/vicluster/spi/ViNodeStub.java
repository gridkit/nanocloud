package org.gridkit.vicluster.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;

class ViNodeStub implements ViNode {

	@Override
	public void execute(Runnable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> userProps() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void exec(Runnable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void exec(VoidCallable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T exec(Callable<T> task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FutureEx<Void> submit(Runnable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FutureEx<Void> submit(VoidCallable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> FutureEx<T> submit(Callable<T> task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> massExec(Callable<? extends T> task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FutureEx<Void>> massSubmit(Runnable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FutureEx<Void>> massSubmit(VoidCallable task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<FutureEx<T>> massSubmit(Callable<? extends T> task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> FutureEx<List<T>> vectorSubmit(Callable<? extends T> task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void label(String label) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> labels() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}
}
