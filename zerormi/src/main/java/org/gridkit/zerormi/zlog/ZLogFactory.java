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

public class ZLogFactory {

	public static ZLogger getStdErrRootLogger() {
		return new PrintStreamLogger("", System.err);
	}

	public static ZLogger getDefaultRootLogger() {
		if ("slf4j".equals(System.getProperty("zlog-mode"))) {
			return getSlf4JRootLogger();
		}
		else {
			return getStdErrRootLogger();
		}
	}

	public static ZLogger getSlf4JRootLogger() {
		try {
			return (ZLogger) Class.forName("org.gridkit.zerormi.zlog.Slf4JLogger").newInstance();
		}
		catch(ClassNotFoundException e) {
			return getStdErrRootLogger();
		}
		catch(NoClassDefFoundError e) {
			return getStdErrRootLogger();
		} 
		catch (InstantiationException e) {
			return getStdErrRootLogger();
		} 
		catch (IllegalAccessException e) {
			return getStdErrRootLogger();
		}
	}	
}
