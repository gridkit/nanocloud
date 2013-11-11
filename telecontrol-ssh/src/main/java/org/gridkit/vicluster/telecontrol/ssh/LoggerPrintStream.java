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
package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.io.PrintStream;

import org.gridkit.zeroio.AbstractLineProcessingOutputStream;
import org.gridkit.zerormi.zlog.LogStream;

class LoggerPrintStream extends PrintStream {

	public LoggerPrintStream(LogStream stream) {
		super(new StreamStub(stream, null));
	}

	public LoggerPrintStream(LogStream stream, String prefix) {
		super(new StreamStub(stream, prefix));
	}
	
	private static final class StreamStub extends AbstractLineProcessingOutputStream {
				
		private final LogStream logger;
		private final String prefix;
		
		private StreamStub(LogStream logger, String prefix) {
			logger.toString(); // null check
			this.logger = logger;
			this.prefix = prefix;
		}

		@Override
		protected void processLine(byte[] data) throws IOException {
			if (logger.isEnabled()) {
				if (prefix == null) {
					logLine(new String(data));
				}
				else {
					logLine(prefix + new String(data));
				}
			}
		}

		private void logLine(String text) {
			if (text.length() > 0) {
				if (text.charAt(text.length() - 1) == '\n') {
					text = text.substring(0, text.length() - 1);
				}
				if (text.charAt(text.length() - 1) == '\r') {
					text = text.substring(0, text.length() - 1);
				}
				logger.log(text);
			}
		}	
	}
}
