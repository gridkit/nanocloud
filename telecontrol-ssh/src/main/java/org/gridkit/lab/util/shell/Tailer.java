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
package org.gridkit.lab.util.shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

class Tailer {
	
	private RandomAccessFile data;
	private long position;
	private List<String> buffer = new ArrayList<String>();
	private StringBuilder remaineder = new StringBuilder();
	
	public Tailer(File file) throws FileNotFoundException {
		data = new RandomAccessFile(file, "r");		
	}
	
	public synchronized String nextLine() throws IOException {
		if (buffer.isEmpty()) {
			feed();
		}
		if (buffer.isEmpty()) {
			return null;
		}
		else {
			return buffer.remove(0);
		}
	}
	
	public void close() {
		buffer.clear();
		try {
			data.close();
		} catch (IOException e) {
			// ignore
		}
	}

	public synchronized String getRemainder() throws IOException {
		return remaineder.toString();
	}

	private synchronized void feed() throws IOException {
		long size = data.length();
		if (position < size) {
			byte[] block = new byte[64 << 10];
			data.seek(position);
			int n = data.read(block);
			position += n;
			for(int i = 0; i != n; ++i) {
				if ('\n'  == block[i]) {
					trimRf(remaineder);
					buffer.add(remaineder.toString());
					remaineder.setLength(0);
				}
				else {
					remaineder.append((char)block[i]);
				}
			}
		}		
	}

	private void trimRf(StringBuilder buf) {
		if (buf.length() > 0 && buf.charAt(buf.length() - 1) == '\r') {
			buf.setLength(buf.length() - 1);
		}
		
	}
}
