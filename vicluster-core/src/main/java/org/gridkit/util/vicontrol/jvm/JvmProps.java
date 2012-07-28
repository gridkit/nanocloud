package org.gridkit.util.vicontrol.jvm;

import org.gridkit.util.vicontrol.ViNodeConfig;

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
	 * Custom clarifer to select JDK
	 */	
	public static String JDK_CLARIFIER = "jdk:clarifier";

	public static void setJvmArg(ViNodeConfig config, String string) {
		config.setProp(JVM_XX + "arg" + string, string);
	}

	public static void setJvmArg(String name, ViNodeConfig config, String string) {
		config.setProp(JVM_XX + name, string);
	}
	
}
