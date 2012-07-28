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
import java.util.Arrays;

public final class RemoteInstance implements Serializable {

	private static final long serialVersionUID = 20090415L;

	String instanceId;
	String[] interfaces;

	public String[] getInterfaces() {
		return interfaces;
	}
	
	public String getInstanceId() {
		return instanceId;
	}

	public RemoteInstance(String instanceId, String[] interfaces) {
		if (instanceId == null) {
			throw new NullPointerException("instanceId cannot be null");
		}
		if (interfaces == null) {
			throw new NullPointerException("interfaces cannot be null");
		}
		this.instanceId = instanceId;
		this.interfaces = interfaces;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RemoteInstance) {
			RemoteInstance ri = (RemoteInstance) obj;
			return instanceId.equals(ri.instanceId) && Arrays.equals(interfaces, ri.interfaces);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return instanceId.hashCode();
	}

	@Override
	public String toString() {
		return "${" + instanceId +"}" + Arrays.toString(interfaces);
	}
}
