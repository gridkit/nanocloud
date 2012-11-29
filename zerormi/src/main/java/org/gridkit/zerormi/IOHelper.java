/**
 * Copyright 2011-2012 Alexey Ragozin
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

import java.io.EOFException;
import java.net.SocketException;


class IOHelper {
	
	public static boolean isSocketTerminationException(Exception e) {
		if (e instanceof EOFException) {
			return true;
		}
		else if (e.getClass() == java.io.IOException.class) {
			if ("pipe is closed by reader".equals(e.getMessage().toLowerCase())) {
				return true;
			}
			if ("pipe is closed by writer".equals(e.getMessage().toLowerCase())) {
				return true;
			}
		}
		else if (e instanceof SocketException) {
			if ("connection reset".equals(e.getMessage().toLowerCase())) {
				return true;
			}
			if ("socket closed".equals(e.getMessage().toLowerCase())) {
				return true;
			}
		}
		
		// otherwise
		return false;
	}

}
