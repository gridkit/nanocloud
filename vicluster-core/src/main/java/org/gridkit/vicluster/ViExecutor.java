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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ViExecutor {

	/**
	 * Sends empty runnable for sync execution. Usefully to force node initialization.
	 */
	public void touch();
	
	public void exec(Runnable task);
	
	public void exec(VoidCallable task);

	public <T> T exec(Callable<T> task);
	
	public Future<Void> submit(Runnable task);
	
	public Future<Void> submit(VoidCallable task);
	
	public <T> Future<T> submit(Callable<T> task);	

	// Mass operations

	/**
	 * Version of exec for group
	 * 
	 * @return
	 */
	public <T> List<T> massExec(Callable<? extends T> task);
	
	public List<Future<Void>> massSubmit(Runnable task);
	
	public List<Future<Void>> massSubmit(VoidCallable task);
	
	public <T> List<Future<T>> massSubmit(Callable<? extends T> task);
}
