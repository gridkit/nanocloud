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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gridkit.util.concurrent.FutureBox;

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

	public static Link link(InputStream is, final OutputStream os, boolean closeOnEof) {
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
			return link(is, wos);
		}
		else {
			return link(is, os);
		}
	}

	public static Link link(InputStream is, OutputStream os) {
		synchronized (BACKLOG) {
			StreamPair link = new StreamPair(is, os);
			BACKLOG.add(link);
			return link;
		}
	}

	public static int pullStream(byte[] buffer, InputStream is, OutputStream os) throws IOException {
		int s = is.read(buffer, 0, 0);
		if (s < 0) {
			return s;
		}
		else if (is.available() > 0) {
			int n = is.read(buffer);
			os.write(buffer, 0, n);
			return n;
		}
		else {
			return 0;
		}
	}

	@Override
	public void run() {
		byte[] buffer = new byte[1 << 14];

		while(true) {
			List<StreamPair> backlog;
			synchronized (BACKLOG) {
				backlog = new ArrayList<BackgroundStreamDumper.StreamPair>(BACKLOG);
				for(StreamPair p: backlog) {
					p.locked.set(true);
				}
			}

			int readCount = 0;
			try {

				for(StreamPair pair: backlog) {
                    Object oldThreadLocal = null;
                    try {
                        oldThreadLocal = ReflectionHelper.setInheritedThreadLocal(Thread.currentThread(), pair.inheritedThreadLocal);
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
						}
						catch(Exception x) {
							// ignore;
						}
						closePair(pair);
					}finally {
                        ReflectionHelper.setInheritedThreadLocal(Thread.currentThread(), oldThreadLocal);
                    }
                }
			}
			finally {
				for(StreamPair p: backlog) {
					p.locked.set(false);
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
		close(pair.os);
		close(pair.is);
		pair.locked.set(false);
		try {
			pair.signal.setData(null);
		}
		catch(IllegalStateException e) {
			// ignore
		}
	}

	private static void close(Closeable o) {
		try {
			o.close();
		} catch (IOException e) {
		}
	}

	public interface Link {

		public void flush();

		public void flushAndClose();

		public void join();
	}

	private static class StreamPair implements Link {

        InputStream is;
        OutputStream os;
        FutureBox<Void> signal = new FutureBox<Void>();
        AtomicBoolean locked = new AtomicBoolean(false);
        Object inheritedThreadLocal;

		public StreamPair(InputStream is, OutputStream os) {
			this.is = is;
			this.os = os;

            inheritedThreadLocal = ReflectionHelper.createInheritedThreadLocal(Thread.currentThread());
        }

		@Override
		public void flush() {
			synchronized (BACKLOG) {
				BACKLOG.remove(this);
			}
			sync();

			try {
				pullStream(new byte[8 << 10], is, os);
			} catch (IOException e) {
				// ignore
			}

			synchronized (BACKLOG) {
				BACKLOG.add(this);
			}
		}

		@Override
		public void flushAndClose() {
			synchronized (BACKLOG) {
				BACKLOG.remove(this);
			}
			sync();

			try {
				pullStream(new byte[8 << 10], is, os);
			} catch (IOException e) {
				// ignore
			}

			close(is);
			close(os);
		}

		private void sync() {
			while(locked.get()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}

		@Override
		public void join() {
			try {
				signal.get();
			} catch (Exception e) {
				// ignore
			}
		}
	}

    private static class ReflectionHelper {
        private static Field inheritableThreadLocalField;
        private static Class<?> threadLocalMapClass;

        private static Constructor<?> threadLocalMapConstructor;

        static {
            try {
                inheritableThreadLocalField = Thread.class.getDeclaredField("inheritableThreadLocals");
                inheritableThreadLocalField.setAccessible(true);

                threadLocalMapClass = inheritableThreadLocalField.getType();

                threadLocalMapConstructor = threadLocalMapClass.getDeclaredConstructor(threadLocalMapClass);
                threadLocalMapConstructor.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static Object createInheritedThreadLocal(Thread thread) {
            try {
                final Object inheritedThreadLocalForParentThread = inheritableThreadLocalField.get(thread);
                if (inheritedThreadLocalForParentThread == null) {
                    return null;
                }
                return threadLocalMapConstructor.newInstance(inheritedThreadLocalForParentThread);
            } catch (Exception e) {
                e.printStackTrace();
                return null; // if by some reason code above does not work (due to changes in private API for example) - just forget about in
            }
        }

        static Object setInheritedThreadLocal(Thread thread, Object inheritableThreadLocal) {
            try {
                final Object oldValue = inheritableThreadLocalField.get(thread);
                inheritableThreadLocalField.set(thread, inheritableThreadLocal);
                return oldValue;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
