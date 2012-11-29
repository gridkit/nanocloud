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

/**
 * Normally ViNode start/shutdown hooks are to be executed inside of ViNode.
 * In certain cases though, host side execution is desirable.
 * Any hook implementing this interface are going to be executed on host.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface HostSideHook extends Runnable {

	/**
	 * @param shutdown @true - startup, @false - shutdown
	 */
	public void hostRun(boolean shutdown);
	
}
