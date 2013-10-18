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

class PrintStreamLogger implements ZLogger {

	private final String root;
	private final PrintStream out;
	
	public PrintStreamLogger(String root, PrintStream out) {
		this.root = root;
		this.out = out;
	}

	@Override
	public ZLogger getLogger(String path) {
		String ln = root;
		if (path != null && path.length() > 0) {
			ln += (ln.length() == 0 ? "" : ".") + path;
		}
		return new PrintStreamLogger(ln, out);
	}

	@Override
	public LogStream get(String path, LogLevel level) {
		String ln = root;
		if (path != null && path.length() > 0) {
			ln += (ln.length() == 0 ? "" : ".") + path;
		}
		return createStream(ln, level);
	}
	
	@Override
	public LogStream fatal() {
		return get("", LogLevel.FATAL);
	}

	@Override
	public LogStream critical() {
		return get("", LogLevel.CRITICAL);
	}

	@Override
	public LogStream warn() {
		return get("", LogLevel.WARN);
	}

	@Override
	public LogStream info() {
		return get("", LogLevel.INFO);
	}

	@Override
	public LogStream verbose() {
		return get("", LogLevel.VERBOSE);
	}

	@Override
	public LogStream debug() {
		return get("", LogLevel.DEBUG);
	}

	@Override
	public LogStream trace() {
		return get("", LogLevel.TRACE);
	}

	private LogStream createStream(String name, LogLevel level) {
		String pattern = "%1$tF %1$tT.%1$tL%1$tz "+ name + " - [" + level.toString() + "] %2$s";
		String propname = "zlog." + name + "." + level.toString().toLowerCase();
		boolean enabled = level == LogLevel.CRITICAL || level == LogLevel.FATAL || level == LogLevel.WARN;		
		LogStream sl = new PropConfiguredLogStream(pattern, out, propname, enabled);
		return sl;
	}
	
	private static class PropConfiguredLogStream extends PrintStreamLogStream {

		private final String propName;
		private final boolean defaultEnabled;
		
		public PropConfiguredLogStream(String pattern, PrintStream ps, String propName, boolean defaultEnabled) {
			super(pattern, ps, false);
			this.propName = propName;
			this.defaultEnabled = defaultEnabled;
		}

		@Override
		public void setEnabled(boolean enabled) {
			System.setProperty(propName, String.valueOf(enabled));
		}

		@Override
		public boolean isEnabled() {
			String val = System.getProperty(propName);
			return val == null ? defaultEnabled : "true".equalsIgnoreCase(System.getProperty(propName));
		}
	}
}
