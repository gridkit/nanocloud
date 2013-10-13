/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.lab.interceptor.test;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class BoolHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.booleanNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.booleanIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.booleanDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.booleanStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.booleanIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(booleanNoArgCall());
		String v7 = String.valueOf(booleanIntegerCall(100000));
		String v8 = String.valueOf(booleanDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(booleanStringCall("123"));
		String v10 = String.valueOf(booleanIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public boolean booleanNoArgCall() {
		return false;
	}

	private boolean booleanIntegerCall(int v) {
		return false;
	}
	
	public boolean booleanDoubleCall(double v) {
		return false;
	}
	
	private boolean booleanStringCall(String v) {
		return false;
	}
	
	public boolean booleanIntArrayCall(int... v) {
		return false;
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
