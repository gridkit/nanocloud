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

import org.gridkit.util.concurrent.AdvancedExecutor;


/**
 * Helper class, hosting and number of methods for handling futures etc.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class MassExec {

	public static void exec(AdvancedExecutor executor, Runnable task) {
		try {
			executor.submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)(e.getCause());
			}
			else if (e.getCause() instanceof Error) {
				throw (Error)(e.getCause());
			}
			else {
				throw new RuntimeException(e.getCause());
			}
		}
	}
	
	/**
	 * Collect result from all futures. If any of futures have thrown exception, other futures will be collected but exception disacred.
	 * @return list of results from futures
	 */
	public static <T> List<? super T> waitAll(List<Future<T>> futures) {
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

	@SuppressWarnings("unchecked")
	public static <T> List<T> collectAll(List<? extends Future<?>> futures) {
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
		return (List<T>)Arrays.asList(results);
	}
	
	public static <T> List<T> singleNodeMassExec(ViExecutor exec, Callable<? extends T> task) {
		return Collections.singletonList((T)exec.exec(task));
	}
	
	public static List<Future<Void>> singleNodeMassSubmit(ViExecutor exec, Runnable task) {
		return Collections.singletonList(exec.submit(task));
	}
	
	public static List<Future<Void>> singleNodeMassSubmit(ViExecutor exec, VoidCallable task) {
		return Collections.singletonList(exec.submit(task));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> List<Future<T>> singleNodeMassSubmit(ViExecutor exec, Callable<? extends T> task) {
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
