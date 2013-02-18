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

import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeProps;

/**
 * Static helper class for setup {@link ViNode} props specific to out-of-process implementations.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmProps implements ViNodeProps {

	/**
	 * Read-only process id of JVM
	 */
	public static String PROC_ID = "jvm:proc-id";
	
	/**
	 * Main classpath entry of JVM
	 */
	public static String CP = "jvm:cp"; 

	/**
	 * JVM classpath extension. Multiple named extensions could be specified.
	 */	
	public static String CP_EX = "jvm:cp:"; 

	/**
	 * Addition command line options for JVM.
	 */	
	public static String JVM_XX = "jvm:xx:";

	/**
	 * Addition command line options for JVM.
	 */	
	public static String JVM_WORK_DIR = "jvm:work-dir";

    /**
     * Addition environment variables for JVM.
     */ 
    public static String JVM_ENV = "jvm:env:";
	
	/**
	 * JDK version
	 */	
	public static String JDK_VERSION = "jdk:version";

	/**
	 * CPU Architecture 32 or 64
	 */	
	public static String JDK_ARCH = "jdk:arch";

	/**
	 * Use JRE instead of JDK
	 */	
	public static String JDK_JRE_ONLY = "jdk:jre-only";

	/**
	 * Use particular vendor
	 */	
	public static String JDK_VENDOR = "jdk:vendor";

	/**
	 * Custom clarifier to select JDK
	 */	
	public static String JDK_CLARIFIER = "jdk:clarifier";

	
	public static JvmProps at(ViConfigurable config) {
		return new JvmProps(config);
	}
	
	private final ViConfigurable config;
	
	protected JvmProps(ViConfigurable config) {
		this.config = config;
	}
	
	@Deprecated
	public static void setJvmArg(ViConfigurable config, String string) {
		config.setProp(JVM_XX + "arg:" + string, string);
	}

	public static void addJvmArg(ViConfigurable config, String string) {
		config.setProp(JVM_XX + "arg:" + string, string);
	}

	public JvmProps addJvmArg(String string) {
		config.setProp(JVM_XX + "arg:" + string, string);
		return this;
	}

	public JvmProps setWorkDir(String workDir) {
		config.setProp(JVM_WORK_DIR, workDir);
		return this;
	}
	
	public static void setJvmArg(String logicalName, ViConfigurable config, String string) {
		config.setProp(JVM_XX + logicalName, string);
	}

	public static void setJvmWorkDir(ViConfigurable config, String workDir) {
		config.setProp(JVM_WORK_DIR, workDir);
	}
	
    public JvmProps setEnv(String string, String value) {
        setEnv(config, string, value);
        return this;
    }

    public static void setEnv(ViConfigurable config, String string, String value) {
        config.setProp(JVM_ENV + string, value);
    }
}
