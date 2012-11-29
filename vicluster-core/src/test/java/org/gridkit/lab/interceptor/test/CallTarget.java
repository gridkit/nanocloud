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

public class CallTarget {

	public static void voidNoArgStaticCall() {
		// noop
	}

	public static void voidBooleanStaticCall(boolean v) {
		// noop
	}

	public static void voidByteStaticCall(byte v) {
		// noop
	}

	public static void voidShortStaticCall(short v) {
		// noop
	}

	public static void voidCharStaticCall(char v) {
		// noop
	}

	public static void voidIntegerStaticCall(int v) {
		// noop
	}

	public static void voidLongStaticCall(long v) {
		// noop
	}

	public static void voidFloatStaticCall(float v) {
		// noop
	}

	public static void voidDoubleStaticCall(double v) {
		// noop
	}

	public static void voidStringStaticCall(String v) {
		// noop
	}

	public static void voidIntArrayStaticCall(int... v) {
		// noop
	}
	
	public static void voidStringArrayStaticCall(String... v) {
		// noop
	}

	public static void voidMixed1StaticCall(long v1, String v2) {		
		// noop
	}

	public static void voidMixed2StaticCall(int v1, double v2, String v3) {		
		// noop
	}
	
	public static void voidMixed3StaticCall(Object[] v1, long v2) {		
		// noop
	}
	

	public static boolean booleanNoArgStaticCall() {
		return false;
	}

	public static boolean booleanIntegerStaticCall(int v) {
		return false;
	}
	
	public static boolean booleanDoubleStaticCall(double v) {
		return false;
	}
	
	public static boolean booleanStringStaticCall(String v) {
		return false;
	}
	
	public static boolean booleanIntArrayStaticCall(int... v) {
		return false;
	}

	public static byte byteNoArgStaticCall() {
		return 10;
	}
	
	public static byte byteIntegerStaticCall(int v) {
		return 10;
	}
	
	public static byte byteDoubleStaticCall(double v) {
		return 10;
	}
	
	public static byte byteStringStaticCall(String v) {
		return 10;
	}
	
	public static byte byteIntArrayStaticCall(int... v) {
		return 10;
	}

	public static short shortNoArgStaticCall() {
		return 1000;
	}
	
	public static short shortIntegerStaticCall(int v) {
		return 1000;
	}
	
	public static short shortDoubleStaticCall(double v) {
		return 1000;
	}
	
	public static short shortStringStaticCall(String v) {
		return 1000;
	}
	
	public static short shortIntArrayStaticCall(int... v) {
		return 1000;
	}

	public static char charNoArgStaticCall() {
		return 'я';
	}
	
	public static char charIntegerStaticCall(int v) {
		return 'я';
	}
	
	public static char charDoubleStaticCall(double v) {
		return 'я';
	}
	
	public static char charStringStaticCall(String v) {
		return 'я';
	}
	
	public static char charIntArrayStaticCall(int... v) {
		return 'я';
	}

	public static int intNoArgStaticCall() {
		return 100000;
	}
	
	public static int intIntegerStaticCall(int v) {
		return 100000;
	}
	
	public static int intDoubleStaticCall(double v) {
		return 100000;
	}
	
	public static int intStringStaticCall(String v) {
		return 100000;
	}
	
	public static int intIntArrayStaticCall(int... v) {
		return 100000;
	}
	
	public static long longNoArgStaticCall() {
		return 1l << 40;
	}
	
	public static long longIntegerStaticCall(int v) {
		return 1l << 40;
	}
	
	public static long longDoubleStaticCall(double v) {
		return 1l << 40;
	}
	
	public static long longStringStaticCall(String v) {
		return 1l << 40;
	}
	
	public static long longIntArrayStaticCall(int... v) {
		return 1l << 40;
	}

	public static float floatNoArgStaticCall() {
		return Float.MAX_VALUE;
	}
	
	public static float floatIntegerStaticCall(int v) {
		return Float.MAX_VALUE;
	}
	
	public static float floatDoubleStaticCall(double v) {
		return Float.MAX_VALUE;
	}
	
	public static float floatStringStaticCall(String v) {
		return Float.MAX_VALUE;
	}
	
	public static float floatIntArrayStaticCall(int... v) {
		return Float.MAX_VALUE;
	}
	
	public static double doubleNoArgStaticCall() {
		return Double.MAX_VALUE;
	}
	
	public static double doubleIntegerStaticCall(int v) {
		return Double.MAX_VALUE;
	}
	
	public static double doubleDoubleStaticCall(double v) {
		return Double.MAX_VALUE;
	}
	
	public static double doubleStringStaticCall(String v) {
		return Double.MAX_VALUE;
	}
	
	public static double doubleIntArrayStaticCall(int... v) {
		return Double.MAX_VALUE;
	}

	public static String stringNoArgStaticCall() {
		return "123";
	}
	
	public static String stringIntegerStaticCall(int v) {
		return "123";
	}
	
	public static String stringDoubleStaticCall(double v) {
		return "123";
	}
	
	public static String stringStringStaticCall(String v) {
		return "123";
	}
	
	public static String stringIntArrayStaticCall(int... v) {
		return "123";
	}

	public static Object objectNoArgStaticCall() {
		return "123";
	}
	
	public static Object objectIntegerStaticCall(int v) {
		return "123";
	}
	
	public static Object objectDoubleStaticCall(double v) {
		return "123";
	}
	
	public static Object objectStringStaticCall(String v) {
		return "123";
	}
	
	public static Object objectIntArrayStaticCall(int... v) {
		return "123";
	}

	public static int[] intArrayNoArgStaticCall() {
		return new int[]{1,2,3};
	}
	
	public static int[] intArrayIntegerStaticCall(int v) {
		return new int[]{1,2,3};
	}
	
	public static int[] intArrayDoubleStaticCall(double v) {
		return new int[]{1,2,3};
	}
	
	public static int[] intArrayStringStaticCall(String v) {
		return new int[]{1,2,3};
	}
	
	public static int[] intArrayIntArrayStaticCall(int... v) {
		return new int[]{1,2,3};
	}
	
}
