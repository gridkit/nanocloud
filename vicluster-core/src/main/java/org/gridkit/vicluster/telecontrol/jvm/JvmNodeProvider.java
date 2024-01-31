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

import java.io.File;
import java.io.IOException;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeCore;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.JvmProcessFactory;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmNodeProvider implements ViNodeProvider {

    protected final JvmProcessFactory factory;

    public JvmNodeProvider(JvmProcessFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean verifyNodeConfig(ViNodeConfig config) {
        // TODO
        return true;
    }

    @Override
    public ViNodeCore createNode(String name, ViNodeConfig config) {
        try {
            JvmConfig jvmConfig = prepareJvmConfig(config);
            ManagedProcess process = factory.createProcess(name, jvmConfig);
            return createViNode(name, config, process);
        } catch (IOException e) {
            // TODO special exception for node creation failure
            throw new RuntimeException("Failed to create node '" + name + "'", e);
        }
    }

    @Override
    public void shutdown() {
        // do nothing
    }

    protected JvmConfig prepareJvmConfig(ViNodeConfig config) {
        JvmConfig jvmConfig = new JvmConfig();
        config.apply(new JvmOptionsInitializer(jvmConfig));
        config.apply(new JvmEnvironmentInitializer(jvmConfig));
        config.apply(new JvmClasspathInitializer(jvmConfig));
        String wd = config.getProp(ViConf.JVM_WORK_DIR);
        if (wd != null) {
            jvmConfig.setWorkDir(wd);
        }
        return jvmConfig;
    }

    protected ViNodeCore createViNode(String name, ViNodeConfig config, ManagedProcess process) throws IOException {
//		return new ViEngineNode(name, config.getInternalConfigMap(), process);
        return new JvmNode(name, config, process);
    }

    private static class JvmOptionsInitializer extends ViNodeConfig.ReplyProps {

        private JvmConfig config;

        public JvmOptionsInitializer(JvmConfig config) {
            super(ViConf.JVM_ARGUMENT);
            this.config = config;
        }

        @Override
        protected void setPropInternal(String propName, String value) {
            // pipe char "|" is used to separate multiple options in single property
            if (value.startsWith("|")) {
                String[] options = value.split("[|]");
                for(String option: options) {
                    if (option.trim().length() > 0) {
                        config.addOption(option);
                    }
                }
            }
            else {
                config.addOption(value);
            }
        }
    }

    private static class JvmEnvironmentInitializer extends ViNodeConfig.ReplyProps {

        private JvmConfig config;

        public JvmEnvironmentInitializer(JvmConfig config) {
            super(ViConf.JVM_ENV_VAR);
            this.config = config;
        }

        @Override
        protected void setPropInternal(String propName, String value) {
            config.setEnv(propName.substring(ViConf.JVM_ENV_VAR.length()), value);
        }
    }

    @SuppressWarnings("deprecation")
    private static class JvmClasspathInitializer extends ViNodeConfig.ReplyProps {

        private JvmConfig config;

        public JvmClasspathInitializer(JvmConfig config) {
            super(JvmProps.CP_ADD, JvmProps.CP_REMOVE);
            this.config = config;
        }

        @Override
        protected void setPropInternal(String propName, String value) {
            if (propName.startsWith(JvmProps.CP_ADD)) {
                String path = value;
                if ("".equals(path)) {
                    path = propName.substring(JvmProps.CP_ADD.length());
                }
                try {
                    path = new File(path).getCanonicalPath();
                } catch (IOException e) {
                    // ignore
                }
                config.classpathAdd(path);
            }
            else if (propName.startsWith(JvmProps.CP_REMOVE)) {
                String path = value;
                if ("".equals(path)) {
                    path = propName.substring(JvmProps.CP_REMOVE.length());
                }
                try {
                    path = new File(path).getCanonicalPath();
                } catch (IOException e) {
                    // ignore
                }
                config.classpathExclude(path);
            }
        }
    }
}
