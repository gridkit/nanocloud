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
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteReturn implements RemoteMessage {

	private static final long serialVersionUID = 20090415L;

	/**
	 * The return is a throwable to be thrown?
	 */
	boolean throwing;
	
	/**
	 * Returning object
	 */
	Object ret;
	
	/**
	 * Call id which generated this return
	 */
	long callId;

	public long getCallId() {
		return callId;
	}

	public Object getRet() {
		return ret;
	}

	public boolean isThrowing() {
		return throwing;
	}

	public RemoteReturn(boolean throwing, Object ret, long callId) {
		this.throwing = throwing;
		this.ret = ret;
		this.callId = callId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RemoteReturn[").append(callId)
				.append(", ").append(throwing).append(", ")
				.append(ret).append("]");
		return builder.toString();
	}

	
}
