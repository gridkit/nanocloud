package org.gridkit.vicluster.isolate.instrumentation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.vicluster.isolate.instrumentation.test.SimpleClass_CRT;
import org.gridkit.vicluster.isolate.instrumentation.test.VoidHookTest_CRT;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class CodeRewriteTest {

	private final static boolean DUMP_ASM = false;
	
	public static Object[] LAST_HOOK_PARAMS;
	public static Object NEXT_RETURN;
	public static Throwable NEXT_ERROR;
	
	public static void recorderHook(int hookId, ExecutionHook.HookContext hook) {
		System.out.println("hooked: " + hook);
		LAST_HOOK_PARAMS = hook.getArguments();
	}

	public static void explosiveHook(int hookId, ExecutionHook.HookContext hook) {
		System.out.println("hooked: " + hook);
		LAST_HOOK_PARAMS = hook.getArguments();
		hook.setError(NEXT_ERROR);
	}
	
	public static void replacerHook(int hookId, ExecutionHook.HookContext hook) {
		System.out.println("hooked: " + hook);
		LAST_HOOK_PARAMS = hook.getArguments();
		hook.setResult(NEXT_RETURN);
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
	public void test_void_rewrite() throws Exception {
		final int[] callSiteCounter = new int[1];
		HookManager hookManager = new TestHookManager() {
			@Override
			public int checkCallsite(String hostClass, String hostMethod, String methodSignature, String targetClass, String targetMethod,	 String targetSignature) {
				if ("voidNoArgStaticCall".equals(targetMethod)) {
					System.out.println("Call site: " + targetClass + "::" + targetMethod + targetSignature);
					return 1;
				}
				else {
					System.out.println("Call site: " + targetClass + "::" + targetMethod + targetSignature);
					++callSiteCounter[0];
					return -1;
				}
			}
			
			
		};
		TestClassLoader cl = new TestClassLoader(getClass().getClassLoader(), hookManager);
		Callable<String> ti = cl.createTestInstance(VoidHookTest_CRT.class);
		ti.call();
		Assert.assertSame(cl, ti.getClass().getClassLoader());
	}
	
	private static class RecordingHookmanager extends TestHookManager {
		@Override
		public String getInvocationTargetMethod() {
			return "recoderHook";
		}
	}

	private static class ExplosiveHookmanager extends TestHookManager {
		@Override
		public String getInvocationTargetMethod() {
			return "explosiveHook";
		}
	}

	private static class ReplacerHookmanager extends TestHookManager {
		
		
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
			return CodeRewriteTest.class.getName().replace('.', '/');
		}

		@Override
		public String getInvocationTargetMethod() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int checkCallsite(String hostClass, String hostMethod,	String methdoSignature, String targetClass,	String targetMethod, String targetSignature) {
			for(int i = 0; i != methods.length; ++i) {
				if (targetMethod.equals(methods[i])) {
					return i;
				}
			}
			return -1;
		}
	}
	
	private static class TestClassLoader extends ClassLoader {
		
		private ClassLoader parent;
		private HookManager manager;
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
