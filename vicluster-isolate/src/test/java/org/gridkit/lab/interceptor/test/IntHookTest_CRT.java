package org.gridkit.lab.interceptor.test;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class IntHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.intNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.intIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.intDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.intStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.intIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(intNoArgCall());
		String v7 = String.valueOf(intIntegerCall(100000));
		String v8 = String.valueOf(intDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(intStringCall("123"));
		String v10 = String.valueOf(intIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public int intNoArgCall() {
		return 100000;
	}
	
	public int intIntegerCall(int v) {
		return 100000;
	}
	
	public int intDoubleCall(double v) {
		return 100000;
	}
	
	public int intStringCall(String v) {
		return 100000;
	}
	
	public int intIntArrayCall(int... v) {
		return 100000;
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
