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
package org.gridkit.zerormi.zlog;

import java.io.PrintStream;

class PrintStreamLogStream extends AbstractLogStream {

	private final PrintStream ps;
	private final String pattern;
	boolean enabled;
	
	public PrintStreamLogStream(String pattern, PrintStream ps, boolean enabled) {
		this.pattern = pattern;
		this.ps = ps;
		this.enabled = enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	protected void logInternal(String msg, Throwable e) {
		String line = String.format(pattern, System.currentTimeMillis(), msg);
		ps.println(line);
		if (e != null) {
			e.printStackTrace(ps);
		}
	}
}
