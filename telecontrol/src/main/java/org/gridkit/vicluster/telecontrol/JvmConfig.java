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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmConfig implements Serializable {

	private static final long serialVersionUID = 20120211L;
	
	private String workDir = null;
	private List<String> jvmOptions = new ArrayList<String>();
	private List<String> classpathChanges = new ArrayList<String>();
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
		classpathChanges.add("+" + path);
	}

	public void classpathExclude(String path) {
		classpathChanges.add("-" + path);
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
	
	public List<ClasspathEntry> filterClasspath(List<ClasspathEntry> classpath) {
		if (classpathChanges.isEmpty()) {
			return classpath;
		}
		else {
			List<ClasspathEntry> entries = new ArrayList<Classpath.ClasspathEntry>(classpath);
			
			for(String change: classpathChanges) {
				if (change.startsWith("+")) {
					String cpe = normalize(change.substring(1));
					addEntry(entries, cpe);
				}
				else if (change.startsWith("-")) {
					String cpe = normalize(change.substring(1));
					removeEntry(entries, cpe);
				}
			}
			
			return entries;
		}
	}
	
	private void addEntry(List<ClasspathEntry> entries, String path) {
		try {
			ClasspathEntry entry = Classpath.getLocalEntry(path);
			if (entry != null) {
				entries.add(entry);
			}
		} catch (IOException e) {
			// TODO logging
		}		
	}

	private void removeEntry(List<ClasspathEntry> entries, String path) {
		Iterator<ClasspathEntry> it = entries.iterator();
		while(it.hasNext()) {
			if (path.equals(normalize(it.next().getUrl()))) {
				it.remove();
			}
		}
	}

	public String filterClasspath(String defaultClasspath) {
		if (classpathChanges.isEmpty()) {
			return defaultClasspath;
		}
		else {
			String fs = System.getProperty("path.separator");
			List<String> pathList = new ArrayList<String>();
			
			for(String cpe: defaultClasspath.split(Pattern.quote(fs))) {
				pathList.add(normalize(cpe));
			}
			
			for(String change: classpathChanges) {
				if (change.startsWith("+")) {
					String cpe = normalize(change.substring(1));
					pathList.add(cpe);
				}
				else if (change.startsWith("-")) {
					String cpe = normalize(change.substring(1));
					// remove all occurences
					pathList.removeAll(Collections.singleton(cpe));
				}
			}

			StringBuilder sb = new StringBuilder();
			for(String cpe: pathList) {
				try {
					// normalize path entry if possible
					cpe = new File(cpe).getCanonicalPath();
				} catch (IOException e) {
					// ignore
				}
				sb.append(cpe).append(fs);
			}
			sb.setLength(sb.length() - fs.length());
			return sb.toString();
		}
	}

	private String normalize(String path) {
		try {
			// normalize path entry if possible
			return new File(path).getCanonicalPath();
		} catch (IOException e) {
			return path;
		}
	}

	private String normalize(URL url) {
		try {
			if (!"file".equals(url.getProtocol())) {
				throw new IllegalArgumentException("Non file URL in classpath: " + url);
			}
			File f = new File(url.toURI());
			String path = f.getPath();
			return normalize(path);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Malformed URL in classpath: " + url);
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
