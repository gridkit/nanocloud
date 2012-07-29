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

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;

/**
 * Static helper class for setup {@link ViNode} props specific to out-of-process implementations.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmProps {

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

	public static void setJvmArg(ViNodeConfig config, String string) {
		config.setProp(JVM_XX + "arg:" + string, string);
	}

	public static void setJvmArg(String name, ViNodeConfig config, String string) {
		config.setProp(JVM_XX + name, string);
	}
	
}
