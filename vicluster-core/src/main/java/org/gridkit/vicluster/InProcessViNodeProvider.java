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
package org.gridkit.vicluster;

import java.util.Map;
import java.util.concurrent.Callable;

import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViNodeExtender;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.zerormi.DirectRemoteExecutor;

public class InProcessViNodeProvider implements ViNodeProvider {

    @Override
    public boolean verifyNodeConfig(ViNodeConfig config) {
        return true;
    }

    @Override
    public ViNodeCore createNode(String name, ViNodeConfig config) {
        InProcessViNode node = new InProcessViNode(name);
        config.apply(node);
        return node;
    }

    @Override
    public void shutdown() {
        // TODO implement shutdown()
    }

    private static class InProcessViNode implements ViNodeCore {

        private Isolate isolate;
        private DirectRemoteExecutor executor;

        public InProcessViNode(String name) {
            isolate = new Isolate(name);
            isolate.start();
            executor = new IsolateDirectExec(isolate);
        }

        @Override
        public <X> X x(ViNodeExtender<X> extention) {
            return extention.wrap(this);
        }

        @Override
        public <X> X x(ViConfExtender<X> extention) {
            return extention.wrap(this);
        }

        @Override
        public String getProp(String propName) {
            return isolate.getProp(propName);
        }

        @Override
        public Object getPragma(String pragmaName) {
            return null;
        }

        @Override
        public void setProp(String propName, String value) {
            isolate.setProp(propName, value);
        }

        @Override
        public void setProps(Map<String, String> props) {
            isolate.setProp(props);
        }

        @Override
        public void setConfigElement(String key, Object value) {
            if (value == null || value instanceof String) {
                setProp(key, (String)value);
            }
        }

        @Override
        public void setConfigElements(Map<String, Object> config) {
            for(String key: config.keySet()) {
                setConfigElement(key, config.get(key));
            }
        }

        @Override
        public void kill() {
            isolate.stop();
        }

        @Override
        public void shutdown() {
            isolate.stop();
        }

        @Override
        public void touch() {
        }

        @Override
        public DirectRemoteExecutor executor() {
            return executor;
        }
    }

    private static class IsolateDirectExec implements DirectRemoteExecutor {

        private final Isolate isolate;

        protected IsolateDirectExec(Isolate isolate) {
            this.isolate = isolate;
        }

        @Override
        public void exec(Runnable task) throws Exception {
            isolate.execNoMarshal(task);
        }

        @Override
        public <V> V exec(final Callable<V> task) throws Exception {
            return isolate.execNoMarshal(task);
        }

        @Override
        public FutureEx<Void> submit(final Runnable task) {
            final FutureBox<Void> box = new FutureBox<>();
            isolate.submitNoMarshal(new Runnable() {

                @Override
                public void run() {
                    box.capture(task);
                }
            });
            return box;
        }

        @Override
        public <V> FutureEx<V> submit(Callable<V> task) {
            final FutureBox<V> box = new FutureBox<>();
            isolate.submitNoMarshal(new Runnable() {

                @Override
                public void run() {
                    box.capture(task);
                }
            });
            return box;
        }
    }
}
