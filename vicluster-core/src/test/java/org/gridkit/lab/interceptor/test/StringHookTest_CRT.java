package org.gridkit.lab.interceptor.test;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class StringHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.stringNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.stringIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.stringDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.stringStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.stringIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(stringNoArgCall());
		String v7 = String.valueOf(stringIntegerCall(100000));
		String v8 = String.valueOf(stringDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(stringStringCall("123"));
		String v10 = String.valueOf(stringIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public String stringNoArgCall() {
		return "123";
	}
	
	public String stringIntegerCall(int v) {
		return "123";
	}
	
	public String stringDoubleCall(double v) {
		return "123";
	}
	
	public String stringStringCall(String v) {
		return "123";
	}
	
	public String stringIntArrayCall(int... v) {
		return "123";
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
