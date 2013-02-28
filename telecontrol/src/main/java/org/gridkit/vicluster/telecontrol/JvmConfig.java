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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmConfig implements Serializable {

	private static final long serialVersionUID = 20120211L;
	
	private String workDir = null;
	private List<String> jvmOptions = new ArrayList<String>();
	private List<String> classpathIncludes = new ArrayList<String>();
	private List<String> classpathExcludes = new ArrayList<String>();
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
	
	public void classpathAdd(String path) {
		classpathIncludes.add(path);
	}

	public void classpathExclude(String path) {
		classpathExcludes.add(path);
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
	
	public String filterClasspath(String defaultClasspath) {
		if (classpathExcludes.isEmpty() && classpathIncludes.isEmpty()) {
			return defaultClasspath;
		}
		else {
			String fs = System.getProperty("path.separator");
			StringBuilder sb = new StringBuilder();
			for(String cpe: defaultClasspath.split(Pattern.quote(fs))) {
				try {
					// normalize path entry if possible
					cpe = new File(cpe).getCanonicalPath();
				} catch (IOException e) {
					// ignore
				}
				if (classpathExcludes.contains(cpe)) {
					continue;
				}
				sb.append(cpe).append(fs);
			}
			for(String cpe: classpathIncludes) {
				sb.append(cpe).append(fs);
			}
			sb.setLength(sb.length() - fs.length());
			return sb.toString();
		}
	}
	
	public void apply(ExecCommand jvmCmd) {
		if (workDir != null) {
			jvmCmd.setWorkDir(workDir);
		}
		for(String option: jvmOptions) {
			jvmCmd.addArg(option);
		}
		for(Map.Entry<String, String> var : enviroment.entrySet()) {
		    jvmCmd.setEnvironment(var.getKey(), var.getValue());
		}
	}	
}
