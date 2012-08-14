package org.gridkit.vicluster.isolate.instrumentation.test;

import java.util.concurrent.Callable;

public class VoidHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {
		CallTarget.voidNoArgStaticCall();
		return "done";
	}
}
