package org.gridkit.lab.interceptor.test;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class DoubleHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.doubleNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.doubleIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.doubleDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.doubleStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.doubleIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(doubleNoArgCall());
		String v7 = String.valueOf(doubleIntegerCall(100000));
		String v8 = String.valueOf(doubleDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(doubleStringCall("123"));
		String v10 = String.valueOf(doubleIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public double doubleNoArgCall() {
		return Double.MAX_VALUE;
	}
	
	public double doubleIntegerCall(int v) {
		return Double.MAX_VALUE;
	}
	
	public double doubleDoubleCall(double v) {
		return Double.MAX_VALUE;
	}
	
	public double doubleStringCall(String v) {
		return Double.MAX_VALUE;
	}
	
	public double doubleIntArrayCall(int... v) {
		return Double.MAX_VALUE;
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
