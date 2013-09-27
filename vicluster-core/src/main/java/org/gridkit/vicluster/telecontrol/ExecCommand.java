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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Information need to start process.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ExecCommand implements Cloneable, Serializable {

	private static final long serialVersionUID = 20090416L;
	
	private String executable;
	private String workDir = ".";
	private Map<String, String> enviroment = new HashMap<String, String>();
	private List<String> arguments = new ArrayList<String>();
	
	public ExecCommand(String executable) {
		this.executable = executable;
	}
	
	public String getExecutable() {
		return executable;
	}
	
	public String getCommand() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(executable);
		for(String arg: arguments) {
			buffer.append(" ");
			if (arg.indexOf(' ') >= 0) {
				buffer.append('"').append(arg).append('"');
			}
			else {
				buffer.append(arg);
			}
		}
		
		return buffer.toString();
	}

	public String[] getCommandArray() {
		List<String> line = new ArrayList<String>(arguments.size() + 1);
		line.add(executable);
		line.addAll(arguments);
		
		return line.toArray(new String[line.size()]);
	}
	
	public ProcessBuilder getProcessBuilder() {
		List<String> line = new ArrayList<String>(arguments.size() + 1);
		line.add(executable);
		line.addAll(arguments);
		ProcessBuilder pb = new ProcessBuilder(line);
		pb.directory(new File(workDir));
		pb.environment().putAll(enviroment);
		return pb;
	}
	
	public String getWorkDir() {
		return workDir;
	}

	public ExecCommand setWorkDir(String workDir) {
		this.workDir = workDir;
		return this;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public ExecCommand addArg(String arg) {
		arguments.add(arg);
		return this;
	}

	public ExecCommand addMultipleArg(String arg) {
		// TODO add proper parenthesis parsing
		String[] args = arg.split("\\s+");
		for(String part: args) {
			arguments.add(part);
		}
		return this;
	}

	public Map<String, String> getEviroment() {
		return enviroment;
	}
	
	public ExecCommand setEnvironment(String name, String value) {
		enviroment.put(name, value);
		return this;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ExecCommand clone() {
		ExecCommand result;
		try {
			result = (ExecCommand) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("impossible");
		}
		result.arguments = (List<String>) ((ArrayList<String>)arguments).clone();
		result.enviroment = (Map<String, String>) ((HashMap<String, String>) enviroment).clone();
		return result;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(workDir == null ? "" : "(" + workDir + ") ").append(executable);
		for(String arg: arguments) {
			buffer.append(" ").append(arg);			
		}
		
		return buffer.toString();
	}

}
