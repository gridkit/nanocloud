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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmConfig implements Serializable {

	private static final long serialVersionUID = 20120211L;
	
	private String workDir = null;
	private List<String> jvmOptions = new ArrayList<String>();
//	private List<String> classpathExtras = new ArrayList<String>();
	private Map<String, String> enviroment = new HashMap<String, String>();
	
	public JvmConfig() {		
	}
	
	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
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
	
	public void setEnv(String name, String value) {
	    enviroment.put(name, value);
	}

	public Map<String, String> getEnviroment() {
        return enviroment;
    }
	
	public void apply(ExecCommand jvmCmd) {
		if (workDir != null) {
			jvmCmd.setWorkDir(workDir);
		}
		for(String option: jvmOptions) {
			jvmCmd.addArg(option);
		}
		for(Map.Entry<String, String> var : enviroment.entrySet()) {
		    jvmCmd.setVar(var.getKey(), var.getValue());
		}
	}	
}
