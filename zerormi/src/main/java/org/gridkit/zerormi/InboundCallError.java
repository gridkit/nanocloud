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
package org.gridkit.zerormi;


/**
 * This object is produced if deserialization of
 * {@link RemoteCall} have failed.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class InboundCallError implements RemoteMessage {

    /**
     * The id is a number unique in client and server to identify the call
     */
    transient long callId;

	/**
	 * Instance will receive the call
	 */
	RemoteInstance remoteInstance;
	
	/**
	 * Method's name
	 * TODO normal method signature
	 */
	RemoteMethodSignature method;
	
	Exception error;
	

	public InboundCallError(long callId, RemoteInstance remoteInstance, RemoteMethodSignature method, Exception error) {
        this.callId = callId;
    	this.remoteInstance = remoteInstance;
    	this.method = method;
    	this.error = error;
    }

    public long getCallId() {
		return callId;
	}

	public RemoteInstance getRemoteInstance() {
		return remoteInstance;
	}

	public RemoteMethodSignature getMethod() {
		return method;
	}

	public Exception getError() {
	    return error;
	}

	@Override
	public String toString() {
		return "RemoteCall{" + method + "}." + callId;
	}	
	
	public String toShortString() {
		return toSimpleName(method.getClassName()) + "#" + method.getMethodName() + "()/" + callId;
	}

	private String toSimpleName(String className) {
		int c = className.lastIndexOf('.');
		if (c >= 0) {
			className = className.substring(c + 1);
		}
		return className;
	}
}
