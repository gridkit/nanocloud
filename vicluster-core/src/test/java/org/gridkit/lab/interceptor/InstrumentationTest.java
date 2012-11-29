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
package org.gridkit.lab.interceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.Assert;

import org.gridkit.lab.interceptor.ViInstrumentation.CutPoint;
import org.junit.Test;

public class InstrumentationTest {

	@Test
	public void test_cut_point_match() throws IOException {
		CutPoint cp_curMillis = ViInstrumentation.methodCutPoint(System.class, "currentTimeMillis");
		CutPoint cp_println_long = ViInstrumentation.methodCutPoint(PrintStream.class, "println", long.class);
		CutPoint cp_println_string = ViInstrumentation.methodCutPoint(PrintStream.class, "println", String.class);
		CutPoint cp_arrays_search = ViInstrumentation.methodCutPoint(Arrays.class, "binarySearch", Object[].class, Object.class);
		
		Collection<CutPoint> cpset = Arrays.asList(cp_curMillis, cp_println_long, cp_println_string, cp_arrays_search);
		
		Collection<CutPoint> testA = ViInstrumentation.match(readClass(TestA.class), cpset);
		Assert.assertTrue(testA.contains(cp_curMillis));
		Assert.assertTrue(testA.contains(cp_println_long));
		Assert.assertFalse(testA.contains(cp_println_string));
		Assert.assertFalse(testA.contains(cp_arrays_search));
		
		Collection<CutPoint> testB = ViInstrumentation.match(readClass(TestB.class), cpset);
		Assert.assertFalse(testB.contains(cp_curMillis));
		Assert.assertFalse(testB.contains(cp_println_long));
		Assert.assertTrue(testB.contains(cp_println_string));
		Assert.assertTrue(testB.contains(cp_arrays_search));
	}
	
//	@Test
//	public void asmify_SimpleClass_CRT() throws Exception {
//		ASMifier.main(new String[]{HookMocks.class.getName()});
//	}
	
	private byte[] readClass(Class<?> type) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream is = type.getClassLoader().getResourceAsStream(type.getName().replaceAll("[.]", "/") + ".class");
		byte[] buf = new byte[4096];
		while(true) {
			int n = is.read(buf);
			if (n < 0) {
				break;
			}
			else {
				bos.write(buf, 0, n);
			}
		}
		is.close();
		return bos.toByteArray();
	}

	public static class TestA implements Runnable {

		@Override
		public void run() {
			System.out.println(System.currentTimeMillis());
		}
	}

	public static class TestB implements Runnable {
		
		@Override
		public void run() {
			System.out.println("Hello world");
			Arrays.binarySearch(new Object[]{}, null);
		}
	}
}
