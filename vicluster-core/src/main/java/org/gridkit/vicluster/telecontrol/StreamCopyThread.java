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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gridkit.util.concurrent.FutureBox;

/**
 * This class provides means for efficient (single thread) polling of
 * multiple  {@link InputStream}s and push to {@link OutputStream}s.
 *
 * Mostly used to gather console output from remote processes.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StreamCopyThread extends Thread implements StreamCopyService {

	private List<StreamPair> backlog = new ArrayList<StreamCopyThread.StreamPair>();
	private boolean terminated = false;

	public StreamCopyThread() {
	    setDaemon(true);
	    setName("BackgroundStreamCopyService");
	    start();
	}

    @Override
    public Link link(InputStream is, final OutputStream os, boolean closeOnEof) {
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

    @Override
    public Link link(InputStream is, OutputStream os) {
        synchronized (this) {
            if (terminated) {
                throw new IllegalStateException("Service is terminated");
            }
            StreamPair link = new StreamPair(is, os);
            backlog.add(link);
            return link;
        }
    }

    @Override
    public void shutdown() {
        synchronized(this) {
            if (!terminated) {
                terminated = true;
            }
            for(StreamPair pair: new ArrayList<StreamPair>(backlog)) {
                pair.flushAndClose();
            }
        }
    }

    private int pullStream(byte[] buffer, InputStream is, OutputStream os) throws IOException {
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

        while(!terminated) {
            List<StreamPair> backlog;
            synchronized (this) {
                backlog = new ArrayList<StreamCopyThread.StreamPair>(this.backlog);
                for(StreamPair p: backlog) {
                    p.locked.set(true);
                }
            }

            int readCount = 0;
            try {

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
                        }
                        catch(Exception x) {
                            // ignore;
                        }
                        closePair(pair);
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
        synchronized (this) {
            backlog.remove(pair);
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

    private class StreamPair implements Link {
        InputStream is;
        OutputStream os;
        FutureBox<Void> signal = new FutureBox<Void>();
        AtomicBoolean locked = new AtomicBoolean(false);

        public StreamPair(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        @Override
        public void flush() {
            synchronized (this) {
                backlog.remove(this);
            }
            sync();

            try {
                pullStream(new byte[8 << 10], is, os);
            } catch (IOException e) {
                // ignore
            }

            synchronized (this) {
                backlog.add(this);
            }
        }

        @Override
        public void flushAndClose() {
            synchronized (this) {
                backlog.remove(this);
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
}
