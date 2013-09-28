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
package org.gridkit.vicluster.telecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestStreamHelper {

	public static void copy(InputStream in, OutputStream out) throws IOException {
		StreamHelper.copy(in, out);
	}
	
	public static void link(InputStream in, OutputStream out) {
		BackgroundStreamDumper.link(in, out, false);
	}

	public static void hardLink(InputStream in, OutputStream out) {
		BackgroundStreamDumper.link(in, out, true);
	}
	
}
