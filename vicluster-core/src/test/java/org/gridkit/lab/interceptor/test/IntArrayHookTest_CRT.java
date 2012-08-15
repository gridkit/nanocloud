package org.gridkit.lab.interceptor.test;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class IntArrayHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.intArrayNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.intArrayIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.intArrayDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.intArrayStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.intArrayIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(intArrayNoArgCall());
		String v7 = String.valueOf(intArrayIntegerCall(100000));
		String v8 = String.valueOf(intArrayDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(intArrayStringCall("123"));
		String v10 = String.valueOf(intArrayIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public int[] intArrayNoArgCall() {
		return new int[]{1,2,3};
	}
	
	public int[] intArrayIntegerCall(int v) {
		return new int[]{1,2,3};
	}
	
	public int[] intArrayDoubleCall(double v) {
		return new int[]{1,2,3};
	}
	
	public int[] intArrayStringCall(String v) {
		return new int[]{1,2,3};
	}
	
	public int[] intArrayIntArrayCall(int... v) {
		return new int[]{1,2,3};
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
