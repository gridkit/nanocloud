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

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface VoidCallable {		

	public void call() throws Exception;
	
	@SuppressWarnings("serial")
	public static class VoidCallableWrapper implements Callable<Void>, Serializable {
		
		public final VoidCallable callable;
		
		public VoidCallableWrapper(VoidCallable callable) {
			this.callable = callable;
		}
	
		@Override
		public Void call() throws Exception {
			callable.call();
			return null;
		}
	}
}