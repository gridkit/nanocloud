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
package org.gridkit.vicluster.telecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class BackgroundStreamDumper implements Runnable {

	private static List<StreamPair> BACKLOG = new ArrayList<BackgroundStreamDumper.StreamPair>();
	
	static {
		Thread worker = new Thread(new BackgroundStreamDumper());
		worker.setDaemon(true);
		worker.setName("BackgroundStreamCopy");
		worker.start();
	}
	
	public static void link(InputStream is, final OutputStream os, boolean closeOnEof) {
		if (!closeOnEof) {
			OutputStream wos = new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					os.write(b);					
				}

				@Override
				public void write(byte[] b) throws IOException {
					os.write(b);
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					os.write(b, off, len);
				}

				@Override
				public void flush() throws IOException {
					os.flush();
				}

				@Override
				public void close() throws IOException {
					// do nothing
				}
			};
			link(is, wos);
		}
		else {
			link(is, os);
		}
	}

	public static void link(InputStream is, OutputStream os) {
		synchronized (BACKLOG) {
			BACKLOG.add(new StreamPair(is, os));
		}
	}
	
	@Override
	public void run() {
		byte[] buffer = new byte[1 << 14];
		
		while(true) {
			List<StreamPair> backlog;
			synchronized (BACKLOG) {
				backlog = new ArrayList<BackgroundStreamDumper.StreamPair>(BACKLOG);
			}
			
			int readCount = 0;
			
			for(StreamPair pair: backlog) {
				try {
					if (pair.is.read(buffer, 0, 0) < 0) {
						// EOF
						closePair(pair);
					}
					else if (pair.is.available() > 0) {
						int n = pair.is.read(buffer);
						if (n < 0) {
							closePair(pair);
						}
						else {
							++readCount;
							pair.os.write(buffer, 0, n);
						}
					}
				}
				catch(IOException e) {
					try {
						PrintStream ps = new PrintStream(pair.os);
						e.printStackTrace(ps);
						ps.close();
						pair.is.close();
					}
					catch(Exception x) {
						// ignore;
					}
					synchronized (BACKLOG) {
						BACKLOG.remove(pair);
					}
				}
			}
			
			if (readCount == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}		
	}


	private void closePair(StreamPair pair) {
		synchronized (BACKLOG) {
			BACKLOG.remove(pair);
		}
		try {
			pair.os.close();
		}
		catch(Exception e) {
			// ignore
		}
	}


	private static class StreamPair {
		InputStream is;
		OutputStream os;
		public StreamPair(InputStream is, OutputStream os) {
			super();
			this.is = is;
			this.os = os;
		}
	}	
}
