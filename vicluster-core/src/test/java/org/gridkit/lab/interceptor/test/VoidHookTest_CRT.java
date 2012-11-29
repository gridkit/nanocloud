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

import java.util.concurrent.Callable;

public class VoidHookTest_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {
		
		CallTarget.voidNoArgStaticCall();
		voidNoArgCall();
		
		CallTarget.voidBooleanStaticCall(true);
		voidBooleanCall(true);
		
		CallTarget.voidByteStaticCall((byte)10);
		voidByteCall((byte)10);

		CallTarget.voidShortStaticCall((short)1000);
		voidShortCall((short)1000);

		CallTarget.voidCharStaticCall('я');
		voidCharCall('я');
		
		CallTarget.voidIntegerStaticCall(100000);
		voidIntegerCall(100000);

		CallTarget.voidLongStaticCall(1l << 40);
		voidLongCall(1l << 40);

		CallTarget.voidFloatStaticCall(Float.MAX_VALUE);
		voidFloatCall(Float.MAX_VALUE);

		CallTarget.voidDoubleStaticCall(Double.MAX_VALUE);
		voidDoubleCall(Double.MAX_VALUE);

		CallTarget.voidStringStaticCall("123");
		voidStringCall("123");

		CallTarget.voidIntArrayStaticCall(1,2,3);
		voidIntArrayCall(1,2,3);

		CallTarget.voidStringArrayStaticCall("12", "34", "56");
		voidStringArrayCall("12", "34", "56");

		CallTarget.voidMixed1StaticCall(1l << 40, "abc");
		voidMixed1Call(1l << 40, "abc");

		CallTarget.voidMixed2StaticCall(100000, Double.MAX_VALUE, "abc");
		voidMixed2Call(100000, Double.MAX_VALUE, "abc");

		CallTarget.voidMixed3StaticCall(new Object[]{"a", 1}, 3l << 40);
		voidMixed3Call(new Object[]{"a", 1}, 3l << 40);
		
		return "done";
	}
	
	// public modifier forces compiler to use INVOKEVIRTUAL
	public void voidNoArgCall() {
		// do nothing
		this.toString(); // verify this is not null
	}

	// private modifier allow complier to use INVOKESPECIAL
	private void voidBooleanCall(boolean v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidByteCall(byte v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidShortCall(short v) {
		// do nothing
		this.toString(); // verify this is not null
	}
	
	private void voidCharCall(char v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidIntegerCall(int v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidLongCall(long v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidFloatCall(float v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidDoubleCall(Double v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidStringCall(String v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidIntArrayCall(int... v) {
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidStringArrayCall(String... v) {
		// do nothing
		this.toString(); // verify this is not null
	}
	
	public void voidMixed1Call(long v1, String v2) {		
		// do nothing
		this.toString(); // verify this is not null
	}

	private void voidMixed2Call(int v1, double v2, String v3) {		
		// do nothing
		this.toString(); // verify this is not null
	}
	
	private void voidMixed3Call(Object[] v1, long v2) {		
		// do nothing
		this.toString(); // verify this is not null
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
}
