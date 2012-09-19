package org.gridkit.lab.interceptor.isolate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.gridkit.lab.interceptor.ByteCodeHelper;
import org.gridkit.lab.interceptor.ClassRewriter;
import org.gridkit.lab.interceptor.HookManager;
import org.gridkit.lab.interceptor.Interceptor;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.IsolateClassTransformer;
import org.gridkit.vicluster.isolate.IsolateClassTransformerSupport;

public class InterceptorClassTransformer implements IsolateClassTransformer {

	private List<ProbeInfo> interceptors = new ArrayList<ProbeInfo>();
	private HookManager hm = new ProbeMatcher();
	private ClassRewriter rewriter = new ClassRewriter(hm);
	private Set<String> blackList = new HashSet<String>();
	
	private Method addHookMethod;
	
	public InterceptorClassTransformer() {
	}
	
	private static class ProbeInfo {
		
		String classname;
		String methodname;
		String[] signature;
		
		int probeId = -1;
		Interceptor probe;		
	}
	
	@Override
	public void init(Isolate host, IsolateClassTransformerSupport support) {
		try {
			Class<?> anchor = support.loadIsolated(InterceptionAnchor.class.getName());
			
			for (ProbeInfo probe: interceptors) {
				final Interceptor instr = probe.probe;
				int pid = host.exec(new Callable<Integer>(){
					@Override
					public Integer call() throws Exception {
						return InterceptionAnchor.registerInterceptor(instr);
					}
				});
				probe.probeId = pid;
			}
			
			addHookMethod = anchor.getMethod("addHook", int[].class);
			addHookMethod.setAccessible(true);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] transform(ClassLoader cl, String name, byte[] originalClass) {
		byte[] newData = rewriter.rewrite(originalClass);
		return newData;
	}

	public void addProbe(Interceptor probe, String cname, String mname, String... params) {
		if (addHookMethod != null) {
			throw new IllegalStateException("Cannot add probes after transformer instalation");
		}
		
		ProbeInfo info = new  ProbeInfo();
		info.classname = cname;
		info.methodname = mname;
		info.signature = params;
		info.probe = probe;
		
		interceptors.add(info);
		blackList.add(info.probe.getClass().getName().replace('.', '/'));
	}
	
	private boolean match(ProbeInfo probe, String cname, String methodName, String[] paramTypes) {
		if (!probe.classname.equals(cname)) {
			return false;
		}
		if (!probe.methodname.equals(methodName)) {
			return false;
		}
		if (probe.signature.length != paramTypes.length) {
			return false;
		}
		for (int i = 0; i != paramTypes.length; ++i) {
			if (!probe.signature[i].equals(paramTypes[i])) {
				return false;
			}
		}
		return true;
	}
	
	private int createHook(int[] probes) {
		try {
			return (Integer)addHookMethod.invoke(null, new Object[]{probes});
		} catch (Exception e) {
			e.printStackTrace();
			// TODO error handling
			return -1;
		}
	}
	
	private class ProbeMatcher implements HookManager {

		@Override
		public String getInvocationTargetClass() {
			return InterceptionAnchor.class.getName().replace('.', '/');
		}

		@Override
		public String getInvocationTargetMethod() {
			return "dispatch";
		}

		@Override
		public int checkCallsite(String hostClass, String hostMethod, String methdoSignature, String targetClass, String targetMethod, String targetSignature) {
			if (addHookMethod == null) {
				// we need to covert probes to Isolate, which may trigger RMI class loading
				// skipping transforming of such classes.
				return -1;
			}
			if (blackList.contains(hostClass)) {
				return -1;
			}
			List<ProbeInfo> matches = new ArrayList<ProbeInfo>();
			for(ProbeInfo probe: interceptors) {
				if (match(probe, targetClass, targetMethod, ByteCodeHelper.parseParamTypeNames(targetSignature))) {
					matches.add(probe);
					System.out.println("call site: " + hostClass + " " + hostMethod);
					System.out.println("  match: " + probe);
				}
			}
			if (!matches.isEmpty()) {
				int[] hi = new int[matches.size()];
				for(int i = 0; i != hi.length; ++i) {
					hi[i] = matches.get(i).probeId;
				}
				return createHook(hi);
			}
			else {
				return -1;
			}
		}		
	}	
}
