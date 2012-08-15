package org.gridkit.lab.interceptor.test;

import java.util.concurrent.Callable;

import org.gridkit.lab.interceptor.ReflectionMethodCallSiteHookContext;

public class HookMocks implements Callable<String> {

	@Override
	public String call() throws Exception {
		return "Hello world!".toUpperCase();
	}
	
	private static long mockhook_1() throws Throwable {
		Object[] arguments = new Object[1];

		ReflectionMethodCallSiteHookContext ctx = new ReflectionMethodCallSiteHookContext();
		ctx.setHostClass(HookMocks.class);
		ctx.setTargetClass(System.class);
		ctx.setTargetMethod("currentTimeMillis");
		ctx.setTargetMethodSignature("()");
		ctx.setParameters(arguments);
		
		onHook(1001, ctx);

		if (!ctx.isResultReady()) {
			return System.currentTimeMillis();
		}
		else {
			Throwable e = ctx.getError();
			if (e != null) {
				throw e;
			}
			else {
				return ((Long)ctx.getResult()).longValue();
			}
		}
	}

	private long mockhook_2(String value) throws Throwable {
		Object[] arguments = new Object[2];
		arguments[0] = this;
		arguments[1] = value;

		ReflectionMethodCallSiteHookContext ctx = new ReflectionMethodCallSiteHookContext();
		ctx.setHostClass(HookMocks.class);
		ctx.setTargetClass(System.class);
		ctx.setTargetMethod("currentTimeMillis");
		ctx.setTargetMethodSignature("()");
		ctx.setParameters(arguments);
		
		onHook(1001, ctx);

		if (!ctx.isResultReady()) {
			return System.currentTimeMillis();
		}
		else {
			Throwable e = ctx.getError();
			if (e != null) {
				throw e;
			}
			else {
				return ((Long)ctx.getResult()).longValue();
			}
		}
	}

	private long mockhook_3(long v1, String value) throws Throwable {
		Object[] arguments = new Object[2];
		arguments[0] = this;
		arguments[1] = v1;
		arguments[2] = value;
		
		ReflectionMethodCallSiteHookContext ctx = new ReflectionMethodCallSiteHookContext();
		ctx.setHostClass(HookMocks.class);
		ctx.setTargetClass(System.class);
		ctx.setTargetMethod("currentTimeMillis");
		ctx.setTargetMethodSignature("()");
		ctx.setParameters(arguments);
		
		onHook(1001, ctx);
		
		if (!ctx.isResultReady()) {
			return System.currentTimeMillis();
		}
		else {
			Throwable e = ctx.getError();
			if (e != null) {
				throw e;
			}
			else {
				return ((Long)ctx.getResult()).longValue();
			}
		}
	}

	private long mockhook_4(byte v1, short v2, char v3, int v4, long v5, float v6, double v7) throws Throwable {
		Object[] arguments = new Object[2];
		arguments[0] = this;
		arguments[1] = v1;
		arguments[2] = v2;
		arguments[3] = v3;
		arguments[4] = v4;
		arguments[5] = v5;
		arguments[6] = v6;
		arguments[7] = v7;
		
		ReflectionMethodCallSiteHookContext ctx = new ReflectionMethodCallSiteHookContext();
		ctx.setHostClass(HookMocks.class);
		ctx.setTargetClass(System.class);
		ctx.setTargetMethod("currentTimeMillis");
		ctx.setTargetMethodSignature("()");
		ctx.setParameters(arguments);
		
		onHook(1001, ctx);
		
		if (!ctx.isResultReady()) {
			return System.currentTimeMillis();
		}
		else {
			Throwable e = ctx.getError();
			if (e != null) {
				throw e;
			}
			else {
				return ((Long)ctx.getResult()).longValue();
			}
		}
	}
	
	public static void onHook(int hookId, ReflectionMethodCallSiteHookContext context) {
		// mock
	}
}
