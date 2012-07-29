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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class RemoteRef implements Serializable {

	private static final long serialVersionUID = 20080415L;
	
	private RemoteInstance identity;

	public RemoteRef(RemoteInstance identity) {
		this.identity = identity;
	}

	public RemoteInstance getIdentity() {
		return identity;
	}

	public String toString() {
		return identity.toString();
	}

	private void writeObject(ObjectOutputStream  out) throws IOException {
		((ObjectOutputStream)out).defaultWriteObject();
	}
	
	private void readObject(ObjectInputStream   in) throws IOException, ClassNotFoundException {
		((ObjectInputStream)in).defaultReadObject();
	}
}
