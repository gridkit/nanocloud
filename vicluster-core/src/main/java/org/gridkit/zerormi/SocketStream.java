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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SocketStream implements DuplexStream {

	private Socket socket;
	
	public SocketStream(Socket socket) {
		this.socket = socket;
	}

	@Override
	public InputStream getInput() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutput() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		return socket.isClosed();
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

	@Override
	public String toString() {
		return socket.toString();
	}
}
