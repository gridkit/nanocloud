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

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
abstract class AbstractLogStream implements LogStream {

	protected abstract void logInternal(String msg, Throwable e);

	
	@Override
	public void log(String message) {
		if (!isEnabled()) {
			return;
		}
		logInternal(message, null);
	}

	@Override
	public void log(Throwable e) {
		if (!isEnabled()) {
			return;
		}
		logInternal(e.toString(), e);
	}

	@Override
	public void log(String message, Throwable e) {
	    if (!isEnabled()) {
	        return;
	    }
	    logInternal(message, e);
	}

	@Override
	public void log(String format, Object... args) {
		if (!isEnabled()) {
			return;
		}
		Throwable e = null;
		if (args.length > 0) {
			if (args[args.length - 1] instanceof Throwable) {
				e = (Throwable) args[args.length - 1];
			}
		}
		String msg;
		try {
			msg = String.format(format, args);
		}
		catch(Exception x) {
			StringBuilder sb = new StringBuilder();
			sb.append(format);
			for(Object a: args) {
				try {
					sb.append(' ').append(String.valueOf(a));
				}
				catch(Exception xx) {
					sb.append(" !").append(e.toString()).append("!");
				}
			}
			msg = sb.toString();
		}
		logInternal(msg, e);
	}
}
