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
package org.gridkit.vicluster.telecontrol.isolate;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.ThreadKiller;

public class CloseableThreadKiller implements ThreadKiller, Serializable {

	private static final long serialVersionUID = 20121101L;

	@Override
	public boolean tryToKill(Isolate isolate, Thread thread) {
		if (thread instanceof Closeable) {
			try {
				((Closeable)thread).close();
			}
			catch(IOException e) {
				// ignore;
			}
			return true;
		}
		else {
			return false;
		}
	}
}
