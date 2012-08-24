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

import java.io.IOException;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.JvmProcessFactory;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmNodeProvider implements ViNodeProvider {

	private JvmProcessFactory factory;
	
	public JvmNodeProvider(JvmProcessFactory factory) {
		this.factory = factory;
	}

	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		// TODO
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		try {		
			JvmConfig jvmConfig = new JvmConfig();
			config.apply(new JvmOptionsInitializer(jvmConfig));
			ControlledProcess process = factory.createProcess(name, jvmConfig);
			return new JvmNode(name, config, process);
		} catch (IOException e) {
			// TODO special exception for node creation failure
			throw new RuntimeException("Failed to create node '" + name + "'", e);
		}		
	}
	
	private static class JvmOptionsInitializer extends ViNodeConfig.ReplyProps {

		private JvmConfig config;
		
		public JvmOptionsInitializer(JvmConfig config) {
			super(JvmProps.JVM_XX);
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
}
