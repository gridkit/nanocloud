/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;


/**
 * Helper class, hosting and number of methods for handling futures etc.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class MassExec {

	/**
	 * Collect result from all futures. If any of futures have thrown exception, other futures will be collected but exception disacred.
	 * @return list of results from futures
	 */
	public static <T> List<? super T> waitAll(List<? extends Future<T>> futures) {
		try {
			Object[] results = new Object[futures.size()];
			int n = 0;
			Exception e = null;
			for(Future<T> f : futures) {
				try {
					try {
						results[n] = f.get();
					}
					catch(ExecutionException ee) {
						// unwrapping ExecutionException
						if (ee.getCause() instanceof Exception) {
							throw (Exception)ee.getCause();
						}
						else {
							throw ee;
						}
					}
				}
				catch(Exception ee) {
					if (e == null) {
						e = ee; // only first exception will be thrown
					}
				}
				++n;
			}
			if (e != null) {
				throw e;
			}
			return Arrays.asList(results);
		} catch (Exception e) {
			AnyThrow.throwUncheked(e);
			return null;
		}
	}

	public static List<Object> collectAll(List<Future<?>> futures) {
		Object[] results = new Object[futures.size()];
		int n = 0;
		for(Future<?> f : futures) {
			try {
				try {
					results[n] = f.get();
				}
				catch(ExecutionException e) {
					// unwrapping ExecutionException
					if (e.getCause() instanceof Exception) {
						throw (Exception)e.getCause();
					}
					else {
						throw e;
					}
				}
			}
			catch(Exception e) {
				results[n] = e;
			}
			++n;
		}
		return Arrays.asList(results);
	}
	
	public static <V> FutureEx<List<V>> vectorFuture(List<FutureEx<V>> futures) {
		final FutureVector<V> vect = new FutureVector<V>(futures.size());
		
		int n = 0;
		for (FutureEx<V> f: futures) {
			final int i = n++;
			
			f.addListener(new Box<V>() {
				@Override
				public void setData(V data) {
					vect.setElement(i, data);					
				}
				@Override
				public void setError(Throwable e) {
					vect.setFailure(e);					
				}
			});
		}
		return vect;
	}
	
	public static <T> List<T> singleNodeMassExec(ViExecutor exec, Callable<? extends T> task) {
		return Collections.singletonList((T)exec.exec(task));
	}
	
	public static List<FutureEx<Void>> singleNodeMassSubmit(ViExecutor exec, Runnable task) {
		return Collections.singletonList(exec.submit(task));
	}
	
	public static List<FutureEx<Void>> singleNodeMassSubmit(ViExecutor exec, VoidCallable task) {
		return Collections.singletonList(exec.submit(task));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> List<FutureEx<T>> singleNodeMassSubmit(ViExecutor exec, Callable<? extends T> task) {
		return (List)Collections.singletonList(exec.submit(task));
	}	

	public static void submitAndWait(ViExecutor exec, Runnable task) {
		try {
			exec.submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			AnyThrow.throwUncheked(e.getCause());
		}
	}
	
	public static void submitAndWait(ViExecutor exec, VoidCallable task) {
		try {
			exec.submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			AnyThrow.throwUncheked(e.getCause());
		}
	}
	
	public static <T> T submitAndWait(ViExecutor exec, Callable<? extends T> task) {
		try {
			return exec.submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			AnyThrow.throwUncheked(e.getCause());
			throw new Error("Unreachable");
		}
	}	
	
	private static class FutureVector<V> extends FutureBox<List<V>> {
		
		private Object[] values;
		private int remains;
		private Throwable lastError;
		
		public FutureVector(int size) {
			values = new Object[size];
			remains = size;
		}
		
		public synchronized void setElement(int n, V v) {
			--remains;
			values[n] = v;
			checkCountDown();
		}
		
		public synchronized void setFailure(Throwable e) {
			--remains;
			lastError = e;			
			checkCountDown();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void checkCountDown() {
			if (remains == 0) {
				if (lastError != null) {
					setError(lastError);
				}
				else {
					setData((List)Arrays.asList(values));
				}
			}
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
