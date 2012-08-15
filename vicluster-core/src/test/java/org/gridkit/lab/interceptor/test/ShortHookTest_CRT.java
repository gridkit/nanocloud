package org.gridkit.lab.interceptor.test;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class ShortHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.shortNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.shortIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.shortDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.shortStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.shortIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(shortNoArgCall());
		String v7 = String.valueOf(shortIntegerCall(100000));
		String v8 = String.valueOf(shortDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(shortStringCall("123"));
		String v10 = String.valueOf(shortIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public short shortNoArgCall() {
		return 1000;
	}
	
	public short shortIntegerCall(int v) {
		return 1000;
	}
	
	public short shortDoubleCall(double v) {
		return 1000;
	}
	
	public short shortStringCall(String v) {
		return 1000;
	}
	
	public short shortIntArrayCall(int... v) {
		return 1000;
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
