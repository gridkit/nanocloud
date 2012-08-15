package org.gridkit.lab.interceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.gridkit.lab.interceptor.test.BoolHookTest_CRT;
import org.gridkit.lab.interceptor.test.ByteHookTest_CRT;
import org.gridkit.lab.interceptor.test.CharHookTest_CRT;
import org.gridkit.lab.interceptor.test.DoubleHookTest_CRT;
import org.gridkit.lab.interceptor.test.FloatHookTest_CRT;
import org.gridkit.lab.interceptor.test.IntArrayHookTest_CRT;
import org.gridkit.lab.interceptor.test.IntHookTest_CRT;
import org.gridkit.lab.interceptor.test.LongHookTest_CRT;
import org.gridkit.lab.interceptor.test.ObjectHookTest_CRT;
import org.gridkit.lab.interceptor.test.ShortHookTest_CRT;
import org.gridkit.lab.interceptor.test.SimpleClass_CRT;
import org.gridkit.lab.interceptor.test.StringHookTest_CRT;
import org.gridkit.lab.interceptor.test.VoidHookTest_CRT;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class ClassRewriterTest {

	private final static boolean DUMP_ASM = false;
	
	public static Object[] LAST_HOOK_PARAMS;
	public static Object NEXT_RETURN;
	public static Throwable NEXT_ERROR;
	
	private static Object[] lastParamsVector(Interception hook) {
		Object[] vec = new Object[hook.getCallParameters().length + 1];
		vec[0] = hook.getThis();
		System.arraycopy(hook.getCallParameters(), 0, vec, 1, hook.getCallParameters().length);
		return vec;
	}

	public static void recorderHook(int hookId, Interception hook) {
		System.out.println("hooked: " + hook);
		LAST_HOOK_PARAMS = lastParamsVector(hook);
	}

	public static void explosiveHook(int hookId, Interception hook) {
		System.out.println("hooked: " + hook);
		LAST_HOOK_PARAMS = lastParamsVector(hook);
		hook.setError(NEXT_ERROR);
	}
	
	public static void replacerHook(int hookId, Interception hook) {
		System.out.println("hooked: " + hook);
		LAST_HOOK_PARAMS = lastParamsVector(hook);
		hook.setResult(NEXT_RETURN);
	}
	
	public static void checkInterception(int hookId, Interception hook) {
		Assert.assertNotNull(hook.getHookType());
		Assert.assertNotNull(hook.getReflectionObject());
		Assert.assertNotNull(hook.getCallParameters());
		Assert.assertNotNull(hook.getHostClass());
		try {
			hook.call();
		} catch (ExecutionException e) {
			throw new AssertionError("Unexpected exception: " + e);
		}		
		
		// try also call method directly
		try {
			Method m = (Method)hook.getReflectionObject();
			m.setAccessible(true);
			
			m.invoke(hook.getThis(), hook.getCallParameters());
		} catch (Exception e) {
			throw new AssertionError("Unexpected exception: " + e);
		}				
	}
	
	@Test
	public void test_noop_rewrite() throws Exception {
		final int[] callSiteCounter = new int[1];
		HookManager hookManager = new RecordingHookmanager() {
			@Override
			public int checkCallsite(String hostClass, String hostMethod, String methdoSignature, String targetClass, String targetMethod,	 String targetSignature) {
				System.out.println("Call site: " + targetClass + "::" + targetMethod + targetSignature);
				++callSiteCounter[0];
				return -1;
			}
		};
		TestClassLoader cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		Callable<String> ti = cl.createTestInstance(SimpleClass_CRT.class);
		ti.call();
		Assert.assertSame(cl, ti.getClass().getClassLoader());
	}

	@Test
	public void record_void_static_no_arg_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidNoArgStaticCall", new Object[1]);
	}

	@Test
	public void record_void_static_boolean_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidBooleanStaticCall", null, true);
	}

	@Test
	public void record_void_static_byte_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidByteStaticCall", null, 10);
	}

	@Test
	public void record_void_static_short_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidShortStaticCall", null, 1000);
	}

	@Test
	public void record_void_static_char_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidCharStaticCall", null, "я");
	}
	
	@Test
	public void record_void_static_int_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_void_static_long_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidLongStaticCall", null, 1l << 40);
	}
	
	@Test
	public void record_void_static_float_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidFloatStaticCall", null, Float.MAX_VALUE);
	}

	@Test
	public void record_void_static_double_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_void_static_string_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidStringStaticCall", null, "123");
	}

	@Test
	public void record_void_static_int_array_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_void_static_string_array_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidStringArrayStaticCall", null, new String[]{"12","34","56"});
	}

	@Test
	public void record_void_static_mixed1_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidMixed1StaticCall", null, 1l << 40, "abc");
	}
	
	@Test
	public void record_void_static_mixed2_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidMixed2StaticCall", null, 100000, Double.MAX_VALUE, "abc");
	}
	
	@Test
	public void record_void_static_mixed3_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidMixed3StaticCall", null, new Object[]{"a", 1}, 3l << 40);
	}
	
	@Test
	public void record_void_no_arg_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidNoArgCall", "VoidHookTest_CRT");
	}

	@Test
	public void record_void_boolean_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidNoArgCall", "VoidHookTest_CRT");
	}
	
	@Test
	public void record_void_byte_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidByteCall", "VoidHookTest_CRT", 10);
	}
	
	@Test
	public void record_void_short_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidShortCall", "VoidHookTest_CRT", 1000);
	}
	
	@Test
	public void record_void_char_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidCharCall", "VoidHookTest_CRT", "я");
	}

	@Test
	public void record_void_int_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidIntegerCall", "VoidHookTest_CRT", 100000);
	}
	
	@Test
	public void record_void_long_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidLongCall", "VoidHookTest_CRT", 1l << 40);
	}
	
	@Test
	public void record_void_float_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidFloatCall", "VoidHookTest_CRT", Float.MAX_VALUE);
	}
	
	@Test
	public void record_void_double_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidDoubleCall", "VoidHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_void_string_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidStringCall", "VoidHookTest_CRT", "123");
	}
	
	@Test
	public void record_void_int_array_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidIntArrayCall", "VoidHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void record_void_string_array_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidStringArrayCall", "VoidHookTest_CRT", new String[]{"12", "34", "56"});
	}

	@Test
	public void record_void_mixed1_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidMixed1Call", "VoidHookTest_CRT", 1l << 40, "abc");
	}

	@Test
	public void record_void_mixed2_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidMixed2Call", "VoidHookTest_CRT", 100000, Double.MAX_VALUE, "abc");
	}

	@Test
	public void record_void_mixed3_call() throws Exception {
		test_recorder_hook(VoidHookTest_CRT.class, "voidMixed3Call", "VoidHookTest_CRT",  new Object[]{"a", 1}, 3l << 40);
	}

	@Test
	public void record_boolean_static_no_arg_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_boolean_static_int_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_boolean_static_double_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_boolean_static_string_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanStringStaticCall", null, "123");
	}

	@Test
	public void record_boolean_static_int_array_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_boolean_no_arg_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanNoArgCall", "BoolHookTest_CRT");
	}
	
	@Test
	public void record_boolean_int_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanIntegerCall", "BoolHookTest_CRT", 100000);
	}
	
	@Test
	public void record_boolean_double_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanDoubleCall", "BoolHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_boolean_string_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanStringCall", "BoolHookTest_CRT", "123");
	}
	
	@Test
	public void record_boolean_int_array_call() throws Exception {
		test_recorder_hook(BoolHookTest_CRT.class, "booleanIntArrayCall", "BoolHookTest_CRT", new int[]{1,2,3});
	}

	@Test
	public void record_byte_static_no_arg_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_byte_static_int_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_byte_static_double_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_byte_static_string_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteStringStaticCall", null, "123");
	}

	@Test
	public void record_byte_static_int_array_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_byte_no_arg_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteNoArgCall", "ByteHookTest_CRT");
	}
	
	@Test
	public void record_byte_int_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteIntegerCall", "ByteHookTest_CRT", 100000);
	}
	
	@Test
	public void record_byte_double_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteDoubleCall", "ByteHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_byte_string_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteStringCall", "ByteHookTest_CRT", "123");
	}
	
	@Test
	public void record_byte_int_array_call() throws Exception {
		test_recorder_hook(ByteHookTest_CRT.class, "byteIntArrayCall", "ByteHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void record_short_static_no_arg_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_short_static_int_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_short_static_double_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_short_static_string_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortStringStaticCall", null, "123");
	}

	@Test
	public void record_short_static_int_array_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_short_no_arg_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortNoArgCall", "ShortHookTest_CRT");
	}
	
	@Test
	public void record_short_int_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortIntegerCall", "ShortHookTest_CRT", 100000);
	}
	
	@Test
	public void record_short_double_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortDoubleCall", "ShortHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_short_string_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortStringCall", "ShortHookTest_CRT", "123");
	}
	
	@Test
	public void record_short_int_array_call() throws Exception {
		test_recorder_hook(ShortHookTest_CRT.class, "shortIntArrayCall", "ShortHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void record_char_static_no_arg_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_char_static_int_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_char_static_double_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_char_static_string_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charStringStaticCall", null, "123");
	}

	@Test
	public void record_char_static_int_array_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_char_no_arg_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charNoArgCall", "CharHookTest_CRT");
	}
	
	@Test
	public void record_char_int_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charIntegerCall", "CharHookTest_CRT", 100000);
	}
	
	@Test
	public void record_char_double_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charDoubleCall", "CharHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_char_string_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charStringCall", "CharHookTest_CRT", "123");
	}
	
	@Test
	public void record_char_int_array_call() throws Exception {
		test_recorder_hook(CharHookTest_CRT.class, "charIntArrayCall", "CharHookTest_CRT", new int[]{1,2,3});
	}

	@Test
	public void record_int_static_no_arg_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_int_static_int_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_int_static_double_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_int_static_string_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intStringStaticCall", null, "123");
	}

	@Test
	public void record_int_static_int_array_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_int_no_arg_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intNoArgCall", "IntHookTest_CRT");
	}
	
	@Test
	public void record_int_int_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intIntegerCall", "IntHookTest_CRT", 100000);
	}
	
	@Test
	public void record_int_double_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intDoubleCall", "IntHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_int_string_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intStringCall", "IntHookTest_CRT", "123");
	}
	
	@Test
	public void record_int_int_array_call() throws Exception {
		test_recorder_hook(IntHookTest_CRT.class, "intIntArrayCall", "IntHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void record_long_static_no_arg_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_long_static_int_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_long_static_double_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_long_static_string_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longStringStaticCall", null, "123");
	}

	@Test
	public void record_long_static_long_array_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_long_no_arg_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longNoArgCall", "LongHookTest_CRT");
	}
	
	@Test
	public void record_long_int_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longIntegerCall", "LongHookTest_CRT", 100000);
	}
	
	@Test
	public void record_long_double_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longDoubleCall", "LongHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_long_string_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longStringCall", "LongHookTest_CRT", "123");
	}
	
	@Test
	public void record_long_int_array_call() throws Exception {
		test_recorder_hook(LongHookTest_CRT.class, "longIntArrayCall", "LongHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void record_float_static_no_arg_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_float_static_int_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_float_static_double_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_float_static_string_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatStringStaticCall", null, "123");
	}

	@Test
	public void record_float_static_float_array_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_float_no_arg_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatNoArgCall", "FloatHookTest_CRT");
	}
	
	@Test
	public void record_float_int_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatIntegerCall", "FloatHookTest_CRT", 100000);
	}
	
	@Test
	public void record_float_double_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatDoubleCall", "FloatHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_float_string_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatStringCall", "FloatHookTest_CRT", "123");
	}
	
	@Test
	public void record_float_int_array_call() throws Exception {
		test_recorder_hook(FloatHookTest_CRT.class, "floatIntArrayCall", "FloatHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void record_double_static_no_arg_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_double_static_int_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_double_static_double_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_double_static_string_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleStringStaticCall", null, "123");
	}

	@Test
	public void record_double_static_double_array_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_double_no_arg_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleNoArgCall", "DoubleHookTest_CRT");
	}
	
	@Test
	public void record_double_int_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleIntegerCall", "DoubleHookTest_CRT", 100000);
	}
	
	@Test
	public void record_double_double_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleDoubleCall", "DoubleHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_double_string_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleStringCall", "DoubleHookTest_CRT", "123");
	}
	
	@Test
	public void record_double_int_array_call() throws Exception {
		test_recorder_hook(DoubleHookTest_CRT.class, "doubleIntArrayCall", "DoubleHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void record_string_static_no_arg_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_string_static_int_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_string_static_double_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_string_static_string_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringStringStaticCall", null, "123");
	}

	@Test
	public void record_string_static_string_array_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_string_no_arg_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringNoArgCall", "StringHookTest_CRT");
	}
	
	@Test
	public void record_string_int_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringIntegerCall", "StringHookTest_CRT", 100000);
	}
	
	@Test
	public void record_string_double_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringDoubleCall", "StringHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_string_string_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringStringCall", "StringHookTest_CRT", "123");
	}
	
	@Test
	public void record_string_int_array_call() throws Exception {
		test_recorder_hook(StringHookTest_CRT.class, "stringIntArrayCall", "StringHookTest_CRT", new int[]{1,2,3});
	}

	@Test
	public void record_object_static_no_arg_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_object_static_int_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_object_static_double_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_object_static_string_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectStringStaticCall", null, "123");
	}

	@Test
	public void record_object_static_object_array_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_object_no_arg_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectNoArgCall", "ObjectHookTest_CRT");
	}
	
	@Test
	public void record_object_int_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectIntegerCall", "ObjectHookTest_CRT", 100000);
	}
	
	@Test
	public void record_object_double_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectDoubleCall", "ObjectHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_object_string_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectStringCall", "ObjectHookTest_CRT", "123");
	}
	
	@Test
	public void record_object_int_array_call() throws Exception {
		test_recorder_hook(ObjectHookTest_CRT.class, "objectIntArrayCall", "ObjectHookTest_CRT", new int[]{1,2,3});
	}	
	
	@Test
	public void record_int_array_static_no_arg_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayNoArgStaticCall", new Object[1]);
	}
	
	@Test
	public void record_int_array_static_int_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayIntegerStaticCall", null, 100000);
	}
	
	@Test
	public void record_int_array_static_double_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayDoubleStaticCall", null, Double.MAX_VALUE);
	}

	@Test
	public void record_int_array_static_string_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayStringStaticCall", null, "123");
	}

	@Test
	public void record_int_array_static_int_array_array_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayIntArrayStaticCall", null, new int[]{1,2,3});
	}

	@Test
	public void record_int_array_no_arg_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayNoArgCall", "IntArrayHookTest_CRT");
	}
	
	@Test
	public void record_int_array_int_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayIntegerCall", "IntArrayHookTest_CRT", 100000);
	}
	
	@Test
	public void record_int_array_double_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayDoubleCall", "IntArrayHookTest_CRT", Double.MAX_VALUE);
	}
	
	@Test
	public void record_int_array_string_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayStringCall", "IntArrayHookTest_CRT", "123");
	}
	
	@Test
	public void record_int_array_int_array_call() throws Exception {
		test_recorder_hook(IntArrayHookTest_CRT.class, "intArrayIntArrayCall", "IntArrayHookTest_CRT", new int[]{1,2,3});
	}
	
	@Test
	public void replace_boolean_static_call() throws Exception {
		test_replacer_hook(BoolHookTest_CRT.class, "booleanIntegerStaticCall", true);
	}

	@Test
	public void replace_boolean_call() throws Exception {
		test_replacer_hook(BoolHookTest_CRT.class, "booleanIntegerCall", true);
	}

	@Test(expected=ClassCastException.class)
	public void replace_boolean_bad_cast_call() throws Exception {
		test_replacer_hook(BoolHookTest_CRT.class, "booleanIntegerCall", new BigDecimal("0"));
	}
	
	@Test
	public void replace_byte_static_call() throws Exception {
		test_replacer_hook(ByteHookTest_CRT.class, "byteIntegerStaticCall", (byte)20);
	}
	
	@Test
	public void replace_byte_call() throws Exception {
		test_replacer_hook(ByteHookTest_CRT.class, "byteIntegerCall", (byte)20);
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_byte_bad_cast_call() throws Exception {
		test_replacer_hook(ByteHookTest_CRT.class, "byteIntegerCall", new BigDecimal("143.1"));
	}

	@Test
	public void replace_short_static_call() throws Exception {
		test_replacer_hook(ShortHookTest_CRT.class, "shortIntegerStaticCall", (short)2000);
	}
	
	@Test
	public void replace_short_call() throws Exception {
		test_replacer_hook(ShortHookTest_CRT.class, "shortIntegerCall", (short)2000);
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_short_bad_cast_call() throws Exception {
		test_replacer_hook(ShortHookTest_CRT.class, "shortIntegerCall", new BigDecimal("143.1"));
	}

	@Test
	public void replace_char_static_call() throws Exception {
		test_replacer_hook(CharHookTest_CRT.class, "charIntegerStaticCall", 'ё');
	}
	
	@Test
	public void replace_char_call() throws Exception {
		test_replacer_hook(CharHookTest_CRT.class, "charIntegerCall", 'ё');
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_schar_bad_cast_call() throws Exception {
		test_replacer_hook(CharHookTest_CRT.class, "charIntegerCall", new BigDecimal("143.1"));
	}
	
	@Test
	public void replace_int_static_call() throws Exception {
		test_replacer_hook(IntHookTest_CRT.class, "intIntegerStaticCall", 200000);
	}
	
	@Test
	public void replace_int_call() throws Exception {
		test_replacer_hook(IntHookTest_CRT.class, "intIntegerCall", 200000);
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_int_bad_cast_call() throws Exception {
		test_replacer_hook(IntHookTest_CRT.class, "intIntegerCall", new BigDecimal("143.1"));
	}
	
	@Test
	public void replace_long_static_call() throws Exception {
		test_replacer_hook(LongHookTest_CRT.class, "longIntegerStaticCall", 5l << 40);
	}
	
	@Test
	public void replace_long_call() throws Exception {
		test_replacer_hook(LongHookTest_CRT.class, "longIntegerCall", 5l << 40);
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_long_bad_cast_call() throws Exception {
		test_replacer_hook(LongHookTest_CRT.class, "longIntegerCall", new BigDecimal("143.1"));
	}
	
	@Test
	public void replace_float_static_call() throws Exception {
		test_replacer_hook(FloatHookTest_CRT.class, "floatIntegerStaticCall", 0.1f);
	}
	
	@Test
	public void replace_float_call() throws Exception {
		test_replacer_hook(FloatHookTest_CRT.class, "floatIntegerCall", 0.1f);
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_float_bad_cast_call() throws Exception {
		test_replacer_hook(FloatHookTest_CRT.class, "floatIntegerCall", new BigDecimal("143.1"));
	}

	@Test
	public void replace_double_static_call() throws Exception {
		test_replacer_hook(DoubleHookTest_CRT.class, "doubleIntegerStaticCall", 0.2d);
	}
	
	@Test
	public void replace_double_call() throws Exception {
		test_replacer_hook(DoubleHookTest_CRT.class, "doubleIntegerCall", 0.2d);
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_double_bad_cast_call() throws Exception {
		test_replacer_hook(DoubleHookTest_CRT.class, "doubleIntegerCall", new BigDecimal("143.1"));
	}

	@Test
	public void replace_string_static_call() throws Exception {
		test_replacer_hook(StringHookTest_CRT.class, "stringIntegerStaticCall", "XY");
	}
	
	@Test
	public void replace_string_call() throws Exception {
		test_replacer_hook(StringHookTest_CRT.class, "stringIntegerCall", "XY");
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_string_bad_cast_call() throws Exception {
		test_replacer_hook(StringHookTest_CRT.class, "stringIntegerCall", new BigDecimal("143.1"));
	}
	
	@Test
	public void replace_int_array_static_call() throws Exception {
		test_replacer_hook(IntArrayHookTest_CRT.class, "intArrayIntegerStaticCall", new int[10]);
	}
	
	@Test
	public void replace_int_array_call() throws Exception {
		test_replacer_hook(IntArrayHookTest_CRT.class, "intArrayIntegerCall", new int[10]);
	}
	
	@Test(expected=ClassCastException.class)
	public void replace_int_array_bad_cast_call() throws Exception {
		test_replacer_hook(IntArrayHookTest_CRT.class, "intArrayIntegerCall", new BigDecimal("143.1"));
	}

	@Test
	public void replace_object_static_call() throws Exception {
		test_replacer_hook(ObjectHookTest_CRT.class, "objectIntegerStaticCall", new BigDecimal("3.14159"));
	}
	
	@Test
	public void replace_object_call() throws Exception {
		test_replacer_hook(ObjectHookTest_CRT.class, "objectIntegerCall", new BigDecimal("3.14159"));
	}

	@Test
	public void explode_object_static_call() throws Exception {
		test_explosive_hook(ObjectHookTest_CRT.class, "objectIntegerStaticCall", new IllegalArgumentException("test"));
	}
	
	@Test
	public void explode_object_call() throws Exception {
		test_explosive_hook(ObjectHookTest_CRT.class, "objectIntegerCall", new IllegalArgumentException("test"));
	}

	@Test
	public void check_interception_object_static_call() throws Exception {
		test_invocation_object(ObjectHookTest_CRT.class, "objectIntegerStaticCall");
	}
	
	@Test
	public void check_interception_object_call() throws Exception {
		test_invocation_object(ObjectHookTest_CRT.class, "objectIntegerCall");
	}
		
	private void test_recorder_hook(Class<?> testClass, String method, Object... expected) throws Exception {
		HookManager hookManager = new RecordingHookmanager(method);
		TestClassLoader cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		Callable<String> ti = cl.createTestInstance(testClass);
		Assert.assertSame(cl, ti.getClass().getClassLoader());

		LAST_HOOK_PARAMS = null;
		ti.call();
		Assert.assertNotNull(LAST_HOOK_PARAMS);
		Assert.assertEquals(toString(expected), toString(LAST_HOOK_PARAMS));
	}

	private void test_replacer_hook(Class<?> testClass, String method, Object injection) throws Exception {
		HookManager hookManager = new RecordingHookmanager(method);
		TestClassLoader cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		Callable<String> ti = cl.createTestInstance(testClass);
		Assert.assertSame(cl, ti.getClass().getClassLoader());
		
		LAST_HOOK_PARAMS = null;
		String result1 = ti.call();
		Assert.assertNotNull(LAST_HOOK_PARAMS);
		Assert.assertFalse(result1.contains(String.valueOf(injection)));
		
		hookManager = new ReplacerHookmanager(method);
		cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		ti = cl.createTestInstance(testClass);
		Assert.assertSame(cl, ti.getClass().getClassLoader());
		
		NEXT_RETURN = injection;
		String result2 = ti.call();
		Assert.assertTrue(result2.contains(String.valueOf(injection)));
	}

	private void test_explosive_hook(Class<?> testClass, String method, Exception e) throws Exception {
		HookManager hookManager = new RecordingHookmanager(method);
		TestClassLoader cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		Callable<String> ti = cl.createTestInstance(testClass);
		Assert.assertSame(cl, ti.getClass().getClassLoader());
		
		LAST_HOOK_PARAMS = null;
		ti.call();
		Assert.assertNotNull(LAST_HOOK_PARAMS);
		
		hookManager = new ExplosiveHookmanager(method);
		cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		ti = cl.createTestInstance(testClass);
		Assert.assertSame(cl, ti.getClass().getClassLoader());
		
		NEXT_ERROR = e;
		try {
			ti.call();
			Assert.assertFalse("Exception expected",true);
		}
		catch(Exception ee) {
			Assert.assertSame(e, ee);
		}
	}
	
	private void test_invocation_object(Class<?> testClass, String method) throws Exception {
		HookManager hookManager = new RecordingHookmanager(method) {
			@Override
			public String getInvocationTargetMethod() {
				return "checkInterception";
			}
		};
		TestClassLoader cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		Callable<String> ti = cl.createTestInstance(testClass);
		Assert.assertSame(cl, ti.getClass().getClassLoader());
		ti.call();
	}

	private String toString(Object[] expected) {
		StringBuilder sb = new StringBuilder();
		for(Object o: expected) {
			if (sb.length() > 1) {
				sb.append(", ");
			}
			if (o instanceof Object[]) {
				sb.append(toString((Object[])o));
			}
			else if (o instanceof int[]) {
				sb.append(Arrays.toString(((int[])o)));
			}
			else {
				sb.append(String.valueOf(o));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	private static class RecordingHookmanager extends TestHookManager {

		public RecordingHookmanager(String... methods) {
			super(methods);
		}

		@Override
		public String getInvocationTargetMethod() {
			return "recorderHook";
		}
	}

	private static class ExplosiveHookmanager extends TestHookManager {

		public ExplosiveHookmanager(String... methods) {
			super(methods);
		}

		@Override
		public String getInvocationTargetMethod() {
			return "explosiveHook";
		}
	}

	private static class ReplacerHookmanager extends TestHookManager {
		
		public ReplacerHookmanager(String... methods) {
			super(methods);
		}

		@Override
		public String getInvocationTargetMethod() {
			return "replacerHook";
		}
	}
	
	private static class TestHookManager implements HookManager {

		private String[] methods;
		
		public TestHookManager(String... methods) {
			this.methods = methods;
		}
		
		@Override
		public String getInvocationTargetClass() {
			return ClassRewriterTest.class.getName().replace('.', '/');
		}

		@Override
		public String getInvocationTargetMethod() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int checkCallsite(String hostClass, String hostMethod,	String methdoSignature, String targetClass,	String targetMethod, String targetSignature) {
			for(int i = 0; i != methods.length; ++i) {
				if (targetMethod.equals(methods[i])) {
					return i + 1;
				}
			}
			return -1;
		}
	}
	
	private static class TestClassLoader extends ClassLoader {
		
		private ClassLoader parent;
		private ClassRewriter rewriter;
		
		public TestClassLoader(ClassLoader parent, HookManager manager) {
			this.parent = parent;
			this.rewriter = new ClassRewriter(manager);
		}

		@SuppressWarnings("unchecked")
		public Callable<String> createTestInstance(Class<?> type) throws Exception {
			String name = type.getName();
			Class<?> ntype = loadClass(name);
			return ((Callable<String>)ntype.newInstance());
		}
		
		@Override
		protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			// First, check if the class has already been loaded
			Class<?> c = findLoadedClass(name);
			if (c == null) {
				if (name.endsWith("_CRT")) {
					c = findClass(name);
				}
				else {
					c = parent.loadClass(name);
				}
			}
			if (resolve) {
			    resolveClass(c);
			}
			return c;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				String path = name.replace('.', '/').concat(".class");
				byte[] data = readClass(path);
				data = rewriter.rewrite(data);
				if (DUMP_ASM) {
					ClassReader cr = new ClassReader(data);
					cr.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out)), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				}
				return defineClass(name, data, 0, data.length);
			} catch (Exception e) {
				throw new ClassNotFoundException(name, e);
			}
		}
		
		private byte[] readClass(String path) throws IOException {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			InputStream is = parent.getResourceAsStream(path);
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
	}
}
