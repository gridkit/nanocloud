package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.List;

public class JvmProcessConfiguration {

	private String name;
	private String jvmPath;
	private String jvmCurrentDir;
	private List<String> jvmOptions = new ArrayList<String>();
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJvmPath() {
		return jvmPath;
	}
	
	public void setJvmPath(String jvmPath) {
		this.jvmPath = jvmPath;
	}
	
	public String getJvmCurrentDir() {
		return jvmCurrentDir;
	}
	
	public void setJvmCurrentDir(String jvmCurrentDir) {
		this.jvmCurrentDir = jvmCurrentDir;
	}
	
	public List<String> getJvmOptions() {
		return jvmOptions;
	}
	
	public void addJvmOptions(List<String> jvmOptions) {
		this.jvmOptions.addAll(jvmOptions);
	}

	public void addJvmOption(String option) {
		this.jvmOptions.add(option);
	}
}
