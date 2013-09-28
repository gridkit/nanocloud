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
package org.gridkit.vicluster.telecontrol.ssh;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class WrapperPrintStream extends FilterOutputStream {

	private String prefix;
	private PrintStream printStream;
	private ByteArrayOutputStream buffer;
	
	public WrapperPrintStream(String prefix, PrintStream printStream) {
		super(printStream);
		this.prefix = prefix;
		this.printStream = printStream;
		this.buffer = new ByteArrayOutputStream();
	}
	
	private void dumpBuffer() throws IOException {
		printStream.append(prefix);
		printStream.write(buffer.toByteArray());
		printStream.flush();
		buffer.reset();
	}
	
	@Override
	public synchronized void write(int c) throws IOException {
		synchronized(printStream) {
			buffer.write(c);
			if (c == '\n') {
				dumpBuffer();
			}
		}
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		synchronized(printStream) {
			for (int i = 0; i != len; ++i) {
				if (b[off + i] == '\n') {
					writeByChars(b, off, len);
					return;
				}
			}
			buffer.write(b, off, len);
		}
	}

	private void writeByChars(byte[] cbuf, int off, int len) throws IOException {
		for (int i = 0; i != len; ++i) {
			write(cbuf[off + i]);
		}
	}

	@Override
	public void close() throws IOException {
		super.flush();
		dumpBuffer();			
	}
}