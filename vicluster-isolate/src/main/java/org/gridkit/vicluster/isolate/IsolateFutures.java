package org.gridkit.vicluster.isolate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.isolate.Isolate.BoxProxy;
import org.gridkit.vicluster.isolate.Isolate.FutureProxy;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class IsolateFutures {
	
	static class BoxEnproxy<V> implements BoxProxy<V> {
		
		private final Box<V> box;

		public BoxEnproxy(Box<V> box) {
			this.box = box;
		}

		@Override
		public void setData(V data) {
			box.setData(data);
		}

		@Override
		public void setError(Throwable e) {
			box.setError(e);
		}
	}

	static class BoxUnproxy<V> implements Box<V> {
		
		private final BoxProxy<V> box;
		
		public BoxUnproxy(BoxProxy<V> box) {
			this.box = box;
		}
		
		@Override
		public void setData(V data) {
			box.setData(data);
		}
		
		@Override
		public void setError(Throwable e) {
			box.setError(e);
		}
	}
	
	static class FutureEnproxy<V> implements FutureProxy<V> {
		
		private final FutureEx<V> future;
		
		public FutureEnproxy(FutureEx<V> future) {
			this.future = future;
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return future.cancel(mayInterruptIfRunning);
		}
		
		@Override
		public boolean isCancelled() {
			return future.isCancelled();
		}
		
		@Override
		public boolean isDone() {
			return future.isDone();
		}
		
		@Override
		public V get() throws InterruptedException, ExecutionException {
			return future.get();
		}
		
		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException,
		ExecutionException, TimeoutException {
			return future.get(timeout, unit);
		}
		
		
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void addListener(BoxProxy<? super V> box) {
			future.addListener(new BoxUnproxy(box));
		}
	}
	
	static class FutureUnproxy<V> implements FutureEx<V> {
		
		private final FutureProxy<V> future;

		public FutureUnproxy(FutureProxy<V> future) {
			this.future = future;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return future.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return future.isCancelled();
		}

		@Override
		public boolean isDone() {
			return future.isDone();
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			return future.get();
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return future.get(timeout, unit);
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void addListener(Box<? super V> box) {
			future.addListener(new BoxEnproxy(box));
		}
	}
}
