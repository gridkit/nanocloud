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
package org.gridkit.nanocloud.interceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;

public class ThrowStub implements Interceptor, Serializable {

	private static final long serialVersionUID = 20130621L;

	private Throwable error;

	public ThrowStub(Throwable error) {
		this.error = error;
	}

	@Override
	public void handle(Interception call) {
		Throwable e = clone(error);
		call.setError(e);
	}

	private Throwable clone(Throwable e) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(e);
			oos.flush();
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bis);
			e = (Throwable) ois.readObject();
		} catch (Exception x) {
			// ignore
		}
		e.setStackTrace(new Exception().getStackTrace());
		return e;
	}
}
