package org.gridkit.vicluster.isolate.btrace;

import net.java.btrace.api.extensions.BTraceExtension;

@BTraceExtension
public class IsolateExt {
	
	public static void println(String text) {
		System.out.println(text);
	}
}
