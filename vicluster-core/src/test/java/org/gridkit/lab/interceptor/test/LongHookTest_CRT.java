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

public class LongHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {

		String v1 = String.valueOf(CallTarget.longNoArgStaticCall());
		String v2 = String.valueOf(CallTarget.longIntegerStaticCall(100000));
		String v3 = String.valueOf(CallTarget.longDoubleStaticCall(Double.MAX_VALUE));
		String v4 = String.valueOf(CallTarget.longStringStaticCall("123"));
		String v5 = String.valueOf(CallTarget.longIntArrayStaticCall(1, 2, 3));
		String v6 = String.valueOf(longNoArgCall());
		String v7 = String.valueOf(longIntegerCall(100000));
		String v8 = String.valueOf(longDoubleCall(Double.MAX_VALUE));
		String v9 = String.valueOf(longStringCall("123"));
		String v10 = String.valueOf(longIntArrayCall(1, 2, 3));
		
		return Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10).toString();
	}
	
	public long longNoArgCall() {
		return 1l << 40;
	}
	
	public long longIntegerCall(int v) {
		return 1l << 40;
	}
	
	public long longDoubleCall(double v) {
		return 1l << 40;
	}
	
	public long longStringCall(String v) {
		return 1l << 40;
	}
	
	public long longIntArrayCall(int... v) {
		return 1l << 40;
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
