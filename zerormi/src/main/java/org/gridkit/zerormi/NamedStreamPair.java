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
package org.gridkit.zerormi;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class NamedStreamPair implements DuplexStream {

	private String name;
	private InputStream in;
	private OutputStream out;
	private boolean closed;
	
	public NamedStreamPair(String name, InputStream in, OutputStream out) {
		this.name = name;
		this.in = in;
		this.out = out;
	}

	@Override
	public InputStream getInput() {
		return in;
	}

	@Override
	public OutputStream getOutput() {
		return out;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public synchronized void close() {
		if (!closed) {
			try {
				in.close();
			}
			catch(Exception e) {
				// ignore;
			}
			try {
				out.close();
			}
			catch(Exception e) {
				// ignore;
			}			
			closed = true;
		}		
	}

	@Override
	public String toString() {
		return name;
	}
}
