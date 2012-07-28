package org.gridkit.util.vicontrol.isolate;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class IsolateProps {

	public static String NAME = "isolate:name";
	/** Use for packages to be isolate */
	public static String PACKAGE = "isolate:package:";
	/** Use for classes to be delegated to parent classloader */
	public static String SHARED = "isolate:shared:";
	/** Use for adding additional URLs to classpath */
	public static String CP_ADD = "isolate:cp-add:";
	/** Use for prohibiting URLs in classpath */
	public static String CP_REMOVE = "isolate:cp-remove:";

}
