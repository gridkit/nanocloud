/**
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
package org.gridkit.fabric.remoting;

import java.io.Serializable;

/**
 * Class that holds method call informations.
 * 
 * @date   05/10/2006 
 * @author lipe
 */
public class RemoteCall implements RemoteMessage, Serializable {

	private static final long serialVersionUID = -4057457700512552099L;

	/**
	 * Instance will receive the call
	 */
	RemoteInstance remoteInstance;
	
	/**
	 * Method's name
	 * TODO normal method signature
	 */
	RemoteMethodSignature method;
	
	/**
	 * Method's arguments
	 */
	Object[] args;
	
	/**
	 * The id is a number unique in client and server to identify the call
	 */
	Long callId;

	public Object[] getArgs() {
		return args;
	}

	public Long getCallId() {
		return callId;
	}

	public RemoteInstance getRemoteInstance() {
		return remoteInstance;
	}

	public RemoteMethodSignature getMethod() {
		return method;
	}

	public RemoteCall(RemoteInstance remoteInstance, RemoteMethodSignature method, Object[] args, Long callId) {
		this.remoteInstance = remoteInstance;
		this.method = method;
		this.args = args;
		this.callId = callId;
	}

	@Override
	public String toString() {
		return "RemoteCall{" + method + "}." + callId;
	}	
}
