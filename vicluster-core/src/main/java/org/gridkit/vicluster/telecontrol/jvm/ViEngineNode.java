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
package org.gridkit.vicluster.telecontrol.jvm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViNodeExtender;
import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViNodeCore;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.zerormi.DirectRemoteExecutor;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@SuppressWarnings("deprecation")
class ViEngineNode implements ViNodeCore {

    private ViEngine engine;
    private DirectRemoteExecutor executor;

    public ViEngineNode(ViEngine engine) {
        this.engine = engine;

        if (engine.isTerminated()) {
            Exception e = engine.getConfig().get(ViConf.ERROR_NODE_BOOTSTRAP);
            if (e == null) {
                throw new RuntimeException("Cannot create node, managed process has been terminated");
            } else {
                throw new RuntimeException("Node initialization exception", e);
            }
        }
        ManagedProcess mp = engine.getConfig().getManagedProcess();
        if (mp == null) {
            throw new RuntimeException("Cannot create node, ManagedProcess is not available");
        }
        executor = mp.getExecutionService();
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
    public void touch() {
        // do nothing
    }

    @Override
    public DirectRemoteExecutor executor() {
        return executor;
    }

    @Override
    public void setProp(final String propName, final String value) {
        setProps(Collections.singletonMap(propName, value));
    }

    @Override
    public void setProps(Map<String, String> props) {
        for(String p: props.keySet()) {
            if (!ViConf.isVanilaProp(p)) {
                throw new IllegalArgumentException("[" + p + "] is not 'vanila' prop");
            }
        }
        final Map<String, String> copy = new LinkedHashMap<String, String>(props);
        AdvExecutor2ViExecutor.exec(executor, new Runnable() {
            @Override
            public void run() {
                for(String name: copy.keySet()) {
                    System.setProperty(name, copy.get(name));
                }
            }
        });
    }

    @Override
    public void setConfigElement(String key, Object value) {
        engine.setPragmas(Collections.singletonMap(key, value));
    }

    @Override
    public void setConfigElements(Map<String, Object> config) {
        engine.setPragmas(config);
    }

    @Override
    public String getProp(final String propName) {
        return (String)engine.getPragma(propName);
    }

    @Override
    public Object getPragma(final String pragmaName) {
        return engine.getPragma(pragmaName);
    }

    @Override
    public void kill() {
        engine.kill();
    }

    @Override
    public void shutdown() {
        engine.shutdown();
    }
}
