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
package org.gridkit.nanocloud.interceptor;

import java.io.PrintStream;
import java.io.Serializable;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;

class PrinterStub implements Interceptor, Serializable {

	private static final long serialVersionUID = 20140112L;
	
	private boolean errStream;
	private boolean printStackTrace;
	private String format;
	

	public PrinterStub(String format) {
		this.format = format;
	}

	public void setPrintToErr(boolean err) {
		errStream = err;
	}

	public void setPrintStackTrace(boolean trace) {
		printStackTrace = trace;
	}
	
	@Override
	public void handle(Interception call) {
		String text;
		try {
			text = String.format(format, call.getCallParameters());
		}
		catch(Throwable e) {
			text = "Print interceptor has failed. Template: [" + format + "] Error: " + e.toString();
		}
		PrintStream ps = errStream ? System.err : System.out;
		ps.println(text);
		if (printStackTrace) {
			new Exception().printStackTrace(ps);
		}
	}	
}
