package org.gridkit.vicluster.telecontrol.bootstraper;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility code to manage OS specific aspects.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SystemHelper {

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
	
	/**
	 * Transforms and conanize path according to local system.
	 * <li>
	 * Expands ~/
	 * </li>
	 * <li>
	 * Replaces {tmp} to local IO temp dir
	 * </li> 
	 */
	public static String normalizePath(String path) throws IOException {
		if (path.startsWith("~/")) {
			String home = System.getProperty("user.home");
			File fp = new File(new File(home), path.substring("~/".length()));
			return fp.getCanonicalPath();
		}
		else if (path.startsWith("{tmp}/")) {
			File tmp = File.createTempFile("mark", "").getAbsoluteFile();
			tmp.delete();
			File fp = new File(tmp.getParentFile(), path.substring("{tmp}/".length()));
			return fp.getCanonicalPath();
		}
		else {
			return new File(path).getCanonicalPath();
		}
	}	
}
