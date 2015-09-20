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

import org.slf4j.Logger;

class Slf4JStream extends AbstractLogStream {

	public enum Level {
		ERROR,
		WARN,
		INFO,
		DEBUG,
		TRACE,
		NONE
	}
	
	private final Logger logger;
	private final Level level;
	
	public Slf4JStream(Logger logger, Level level) {
		this.logger = logger;
		this.level = level;
	}

	@Override
	public boolean isEnabled() {
		switch(level) {
		case NONE: return false;
		case ERROR: return logger.isErrorEnabled();
		case WARN: return logger.isWarnEnabled();
		case INFO: return logger.isInfoEnabled();
		case DEBUG: return logger.isDebugEnabled();
		case TRACE: return logger.isTraceEnabled();
		default: throw new Error("No such enum element: " + level);
		}
	}

    @Override
    @SuppressWarnings("incomplete-switch")
	protected void logInternal(String msg, Throwable e) {
		if (e == null) {
			switch(level) {
				case ERROR: logger.error(msg); break;
				case WARN: logger.warn(msg); break;
				case INFO: logger.info(msg); break;
				case DEBUG: logger.debug(msg); break;
				case TRACE: logger.trace(msg); break;
			}
		}
		else {
				switch(level) {
				case ERROR: logger.error(msg, e); break;
				case WARN: logger.warn(msg, e); break;
				case INFO: logger.info(msg, e); break;
				case DEBUG: logger.debug(msg, e); break;
				case TRACE: logger.trace(msg, e); break;
			}
		}
	}
}
