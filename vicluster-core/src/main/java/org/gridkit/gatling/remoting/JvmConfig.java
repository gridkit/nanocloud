package org.gridkit.gatling.remoting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.fabric.exec.ExecCommand;

public class JvmConfig implements Serializable {

	private static final long serialVersionUID = 20120211L;
	
	private List<String> jvmOptions = new ArrayList<String>();
//	private List<String> classpathExtras = new ArrayList<String>();
	
	public JvmConfig() {		
	}
	
	public List<String> getJvmOptions() {
		return jvmOptions;
	}

	public void addOption(String option) {
		if (!option.startsWith("-")) {
			throw new IllegalArgumentException("bad JVM option '" + option + "'");
		}
		jvmOptions.add(option);		
	}

	public void apply(ExecCommand jvmCmd) {
		for(String option: jvmOptions) {
			jvmCmd.addArg(option);
		}		
	}	
}
