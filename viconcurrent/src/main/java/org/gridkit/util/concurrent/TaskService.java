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
package org.gridkit.util.concurrent;

import java.util.concurrent.TimeUnit;

public interface TaskService {

	public void schedule(Task task);

	public void schedule(Task task, long delay, TimeUnit tu);
	
	/**
	 * If {@link TaskService#schedule(Task)} method havn't thrown an
	 * exception, it is guaranteed that eigther {@link #run()} or {@link #canceled()}
	 * would be called eventually.
	 * 
	 * {@link #interrupt(Thread)} may be used to abort task execution, beware that it could be called when {@link #run()} is already finished or have yet to be started.
	 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
	 */
	public interface Task {
		
		public void run();

		public void interrupt(Thread taskThread);
		
		public void canceled();
		
	}
	
	public interface Component extends TaskService {
		
		public void shutdown();
		
	}
}
