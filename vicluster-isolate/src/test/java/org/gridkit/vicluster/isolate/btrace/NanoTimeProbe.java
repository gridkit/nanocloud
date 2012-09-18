package org.gridkit.vicluster.isolate.btrace;

import net.java.btrace.annotations.*;

@BTrace
public class NanoTimeProbe {

	@OnMethod(clazz="/.*/", method="/.*/", location=@Location(value=Kind.CALL, clazz="java.lang.System", method="nanoTime"))
	public static void callSystemNanotime() {
		IsolateExt.println("PING");
	}

}
