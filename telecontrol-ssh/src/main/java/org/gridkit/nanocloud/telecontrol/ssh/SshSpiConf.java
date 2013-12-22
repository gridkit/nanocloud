package org.gridkit.nanocloud.telecontrol.ssh;

public interface SshSpiConf {

//	public static final String REMOTE_BOOTSTRAP_JAR_CACHE = "remote-runtime:bootstrap-jar-cache"; 
	public static final String REMOTE_JAR_CACHE = "remote-runtime:jar-cache"; 

	public static final String REMOTE_FALLBACK_JVM_EXEC = "remote-runtime:fallback-jvm-exec"; 
	
	// TODO sane default managment
	public static final String SPI_BOOTSTRAP_JVM_EXEC = "#spi:remote-runtime:bootstrap-jvm-exec";
	public static final String SPI_JAR_CACHE = "#spi:remote-runtime:jar-cache";

	public static final String SPI_SSH_TARGET_HOST = "#spi:ssh:target-host";
	public static final String SPI_SSH_TARGET_ACCOUNT = "#spi:ssh:target-account";
	
	public static final String SPI_SSH_PASSWORD = "#spi:ssh:password";
	public static final String SPI_SSH_PRIVATE_KEY_FILE = "#spi:ssh:private-key-file";
	public static final String SPI_SSH_JSCH_OPTION = "#spi:ssh:jsch:";

	public static final String SPI_SSH_CONTROL_CONSOLE = "#spi:ssh:control-console";
	
	public static final String SSH_PASSWORD = "ssh:password";
	public static final String SSH_PRIVATE_KEY_FILE = "ssh:private-key-file";
	public static final String SSH_JSCH_OPTION = "ssh:jsch:";
	
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_PRIVATE_KEY_FILE = "private-key";
	public static final String KEY_ADDRESS = "address";
	public static final String KEY_JSCH_PREFERED_AUTH = "jsch-auth";
	public static final String KEY_JAR_CACHE = "jar-cache-path";
	public static final String KEY_JAVA_EXEC = "java-exec";
	
}
