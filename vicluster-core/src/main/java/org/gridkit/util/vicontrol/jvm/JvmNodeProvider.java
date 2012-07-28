package org.gridkit.util.vicontrol.jvm;

import java.io.IOException;

import org.gridkit.gatling.remoting.ControlledProcess;
import org.gridkit.gatling.remoting.JvmConfig;
import org.gridkit.gatling.remoting.JvmProcessFactory;
import org.gridkit.util.vicontrol.ViNode;
import org.gridkit.util.vicontrol.ViNodeConfig;
import org.gridkit.util.vicontrol.ViNodeProvider;

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
			ControlledProcess process = factory.createProcess(jvmConfig);
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
