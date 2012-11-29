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
package org.gridkit.zeroio;

import java.io.IOException;
import java.io.PrintStream;

import org.slf4j.Logger;

public class LoggerPrintStream extends PrintStream {

	public enum Level {
		ERROR,
		WARN,
		INFO,
		DEBUG,
		TRACE,
		NONE
	}
	
	public LoggerPrintStream(Logger logger, Level level) {
		super(new StreamStub(logger, level, null));
	}

	public LoggerPrintStream(Logger logger, Level level, String prefix) {
		super(new StreamStub(logger, level, prefix));
	}
	
	private static final class StreamStub extends AbstractLineProcessingOutputStream {
				
		private final Logger logger;
		private final Level level;	
		private final String prefix;
		
		private StreamStub(Logger logger, Level level, String prefix) {
			level.toString(); // null check
			this.logger = logger;
			this.level = level;			
			this.prefix = prefix;
		}

		@Override
		protected void processLine(byte[] data) throws IOException {
			if (level != Level.NONE) {
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
				switch(level) {
					case ERROR: logger.error(text); break;
					case WARN: logger.warn(text); break;
					case INFO: logger.info(text); break;
					case DEBUG: logger.debug(text); break;
					case TRACE: logger.trace(text); break;
				}
			}
		}	
	}
}
