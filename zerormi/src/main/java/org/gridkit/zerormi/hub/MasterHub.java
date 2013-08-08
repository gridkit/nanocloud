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

import java.util.concurrent.ExecutorService;

import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.hub.RemotingHub.SessionEventListener;

/**
 * <p>
 * Master hub encapsulates logic of establishing ZeroRMI
 * control connection with remote entity.
 * </p>
 * 
 * <p>
 * Details of transport are hidden away, established socket
 * connection is provided to both {@link MasterHub} and its
 * {@link SlaveSpore}.
 * </p> 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface MasterHub {

	public SlaveSpore allocateSession(String sessionId, SessionEventListener listener);

	public ExecutorService getExecutionService(String sessionId);

	public void dispatch(DuplexStream stream);

	public void dropSession(String sessionId);

	public void dropAllSessions();
}
