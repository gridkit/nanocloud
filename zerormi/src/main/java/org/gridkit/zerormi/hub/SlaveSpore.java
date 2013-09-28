/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.zerormi.hub;

import java.io.Serializable;

import org.gridkit.zerormi.DuplexStreamConnector;

/**
 * Spore is a small piece of remote agent configuration allowing
 * it to attach itself to {@link MasterHub}.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface SlaveSpore extends Serializable {

	/**
	 * Spore should connector {@link MasterHub} using provided connector.
	 * It will not return from this call until its death.
	 */
	public void start(DuplexStreamConnector masterConnector);
	
}
