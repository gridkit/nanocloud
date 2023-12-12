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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.Arrays;

/**
 * This is adapter for accessing {@link OutputStream} via remote node boundary.
 *
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@SuppressWarnings("serial")
public class OutputStreamRemoteAdapter extends OutputStream implements Serializable {

    private final transient OutputStream sink;
    private final RemoteOutputInterface proxy;

    public OutputStreamRemoteAdapter(OutputStream sink) {
        this.sink = sink;
        this.proxy = new StreamProxy();
    }

    @Override
    public void write(int b) throws IOException {
        proxy.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        proxy.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        proxy.write(Arrays.copyOfRange(b, off, off + len));
    }

    @Override
    public void flush() throws IOException {
        proxy.flush();
    }

    @Override
    public void close() throws IOException {
        proxy.close();
    }

    private static interface RemoteOutputInterface extends Remote {

        public void write(int b) throws IOException;

        public void write(byte[] b) throws IOException;

        public void flush() throws IOException;

        public void close() throws IOException;
    }

    private class StreamProxy implements RemoteOutputInterface {

        @Override
        public void write(int b) throws IOException {
            sink.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            sink.write(b);
        }

        @Override
        public void flush() throws IOException {
            sink.flush();
        }

        @Override
        public void close() throws IOException {
            sink.close();
        }
    }
}
