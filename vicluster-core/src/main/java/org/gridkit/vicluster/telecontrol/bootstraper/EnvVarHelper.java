package org.gridkit.vicluster.telecontrol.bootstraper;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnvVarHelper {

	private static final boolean CASE_INSENSITIVE_VARS = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase().startsWith("windows");
	
	public static String[] buildInheritedEnvironment(Map<String, String> varOverrides) {
		if (varOverrides == null || varOverrides.isEmpty()) {
			// Runtime.exec() will inherit all vars in case of null
			return null;
		}
		else {
			Map<String, String> vars = new LinkedHashMap<String, String>(System.getenv());
			Map<String, String> nmap = null;
			if (CASE_INSENSITIVE_VARS) {
				nmap = new HashMap<String, String>();
				for(String key: varOverrides.keySet()) {
					nmap.put(key.toUpperCase(), key);
				}
				for(String key: vars.keySet()) {
					nmap.put(key.toUpperCase(), key);
				}
			}
			
			for(String var: varOverrides.keySet()) {
				String normName = nmap == null ? var : nmap.get(var.toUpperCase());
				if (varOverrides.get(var) == null) {
					vars.remove(normName);
				}
				else {
					vars.put(normName, varOverrides.get(var));
				}
			}
			List<String> varArray = new ArrayList<String>();
			for(String var: vars.keySet()) {
				varArray.add(var + "=" + vars.get(var));
			}
			return varArray.toArray(new String[varArray.size()]);
		}
	}
	
}
