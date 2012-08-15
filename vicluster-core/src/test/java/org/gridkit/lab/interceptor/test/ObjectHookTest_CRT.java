package org.gridkit.lab.interceptor.test;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class ObjectHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.objectNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.objectIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.objectDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.objectStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.objectIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(objectNoArgCall());
		String v7 = String.valueOf(objectIntegerCall(100000));
		String v8 = String.valueOf(objectDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(objectStringCall("123"));
		String v10 = String.valueOf(objectIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public Object objectNoArgCall() {
		return "123";
	}
	
	public Object objectIntegerCall(int v) {
		return "123";
	}
	
	public Object objectDoubleCall(double v) {
		return "123";
	}
	
	public Object objectStringCall(String v) {
		return "123";
	}
	
	public Object objectIntArrayCall(int... v) {
		return "123";
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
