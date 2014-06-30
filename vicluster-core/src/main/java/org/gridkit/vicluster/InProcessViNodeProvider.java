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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.gridkit.vicluster.isolate.Isolate;

public class InProcessViNodeProvider implements ViNodeProvider {

	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		InProcessViNode node = new InProcessViNode(name);
		config.apply(node);
		return node;
	}
	
	@Override
	public void shutdown() {
		// TODO implement shutdown()
	}

	private static class InProcessViNode implements ViNode {
		
		private Isolate isolate;
		
		public InProcessViNode(String name) {
			isolate = new Isolate(name);
			isolate.start();
		}

		@Override
		public <X> X x(ViNodeExtender<X> extention) {
		    return extention.wrap(this);
		}

		@Override
		public <X> X x(ViConfExtender<X> extention) {
			return extention.wrap(this);
		}

		@Override
		public String getProp(String propName) {
			return isolate.getProp(propName);
		}

		@Override
		public Object getPragma(String pragmaName) {
			return null;
		}

		public void setProp(String propName, String value) {
			isolate.setProp(propName, value);
		}

		public void setProps(Map<String, String> props) {
			isolate.setProp(props);
		}

		@Override
		public void setConfigElement(String key, Object value) {
			if (value == null || value instanceof String) {
				setProp(key, (String)value);
			}
		}

		@Override
		public void setConfigElements(Map<String, Object> config) {
			for(String key: config.keySet()) {
				setConfigElement(key, config.get(key));
			}
		}
		
		@Override
		public void kill() {
			isolate.stop();
		}

		@Override
		public void shutdown() {
			isolate.stop();
		}

		@Override
		public void touch() {
		}

		@Override
		public void exec(Runnable task) {
			isolate.execNoMarshal(task);
		}

		@Override
		public void exec(final VoidCallable task) {
			isolate.execNoMarshal(new Runnable() {
				@Override
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
		public <T> T exec(Callable<T> task) {
			Future<T> f = submit(task);
			try {
				return f.get();
			} catch (InterruptedException e) {
				AnyThrow.throwUncheked(e);
				throw new Error("Unreachable");
			} catch (ExecutionException e) {
				AnyThrow.throwUncheked(e.getCause());
				throw new Error("Unreachable");
			}
		}

		@Override
		public Future<Void> submit(Runnable task) {
			return (Future<Void>) isolate.submitNoMarshal(task);
		}

		@Override
		public Future<Void> submit(final VoidCallable task) {			
			return (Future<Void>) isolate.submitNoMarshal(new Runnable(){
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
			FutureTask<T> ft = new FutureTask<T>(task);
			isolate.submitNoMarshal(ft);
			return ft;
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
}
