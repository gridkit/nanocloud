package org.gridkit.nanocloud.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.gridkit.lab.interceptor.ClassRewriter;
import org.gridkit.lab.interceptor.CutPoint;
import org.gridkit.lab.interceptor.HookManager;
import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;
import org.gridkit.nanocloud.instrumentation.ByteCodeTransformer;
import org.gridkit.vicluster.isolate.Isolate;

public class IsolateInstrumentationSupport implements HookManager, ByteCodeTransformer {

	private List<InstrumentationRule> allRules = new ArrayList<InstrumentationRule>();
	private List<SiteHook> hookStacks = new ArrayList<SiteHook>();
	private ClassRewriter rewriter = new ClassRewriter(this);

	/**
	 * This is a callback method for byte code instrumenter.
	 * @deprecated
	 */
	@Deprecated
	public static void __intercept(int hookId, Interception hook) throws ExecutionException {
		Isolate is = Isolate.getIsolate(hook.getHostClass().getClassLoader());
		if (is == null) {
			reportHookError("Cannot detect context Isolate for class " + hook.getHostClass().getName());
		}
		IsolateInstrumentationSupport handler = (IsolateInstrumentationSupport) is.getGlobal(IsolateInstrumentationSupport.class, "instance");
		if (handler == null) {
			reportHookError("Cannot find instrumentation handler for class " + hook.getHostClass().getName());
		}
		handler.invoke(hookId, hook);
	}
	
	private static void reportHookError(Object msg) {
		System.out.println("Error calling instrumentation hook" + (msg == null ? "" : ": " + msg.toString()));
	}
	
	@Override
	public String getInvocationTargetClass() {
		return getClass().getName().replace('.', '/');
	}
	
	@Override
	public String getInvocationTargetMethod() {
		return "__intercept";
	}
	
	public synchronized void addRule(InstrumentationRule rule) {
		for(SiteHook hook: hookStacks) {
			if (hook.matches(rule.cutPoint)) {
				hook.add(rule);
			}
		}
		allRules.add(rule);
	}
	
	@Override
	public byte[] rewriteClassData(String className, byte[] byteCode, HierarchyGraph graph) {
		return rewriter.rewrite(byteCode);
	}

	public static IsolateInstrumentationSupport getInstrumentationContext(Isolate isolate) {
		synchronized(isolate) {
			IsolateInstrumentationSupport iis = (IsolateInstrumentationSupport) isolate.getGlobal(IsolateInstrumentationSupport.class, "instance");
			if (iis == null) {
				iis = new IsolateInstrumentationSupport();
				iis.deploy(isolate);
			}
			return iis;
		}
	}
	
	protected void deploy(Isolate isolate) {
		synchronized(isolate) {
			isolate.addClassRule(CutPoint.class.getName(), false);
			isolate.addClassRule(Interceptor.class.getName(), false);
			isolate.addClassRule(Interception.class.getName(), false);
			isolate.setByteCodeTransformer(this);
			isolate.setGlobal(IsolateInstrumentationSupport.class, "instance", this);
		}
	}
	
	public synchronized void addInstrumenationRule(CutPoint cutPoint, Interceptor interceptor) {
		addRule(new InstrumentationRule(cutPoint, interceptor));
	}
	
	@Override
	public synchronized int checkCallsite(String hostClass, String hostMethod, String methodSignature, String targetClass, String targetMethod, String targetSignature) {
		SiteHook hook = null;
		for(InstrumentationRule rule: allRules) {
			if (rule.cutPoint.evaluateCallSite(hostClass, hostMethod, methodSignature, targetClass, targetMethod, targetSignature)) {
				if (hook == null) {
					hook = new SiteHook(hostClass, hostMethod, methodSignature, targetClass, targetMethod, targetSignature);
				}
				hook.add(rule);
			}
		}
		
		if (hook != null) {
			hookStacks.add(hook);
			return hookStacks.indexOf(hook);
		}
		else {
			return -1;
		}
	}

	public void invoke(int hookId, Interception call) {
		try {
			SiteHook stack = hookStacks.get(hookId);
			for(InstrumentationRule rule: stack.rules) {
				try {
					rule.interceptor.handle(call);
				}
				catch(Exception e) {
					reportHookError(e);
				}
			}
		}
		catch(Exception e) {
			reportHookError(e);
		}
	}
	
	private static class InstrumentationRule {
		
		private final CutPoint cutPoint;
		private final Interceptor interceptor;
		
		public InstrumentationRule(CutPoint cutPoint, Interceptor interceptor) {
			this.cutPoint = cutPoint;
			this.interceptor = interceptor;
		}
	}

	private static class SiteHook {
		
		String hostClass;
		String hostMethod;
		String methodSignature;
		String targetClass;
		String targetMethod;
		String targetSignature;
		
		volatile List<InstrumentationRule> rules;
		
		public SiteHook(String hostClass, String hostMethod, String methodSignature, String targetClass, String targetMethod, String targetSignature) {
			this.hostClass = hostClass;
			this.hostMethod = hostMethod;
			this.methodSignature = methodSignature;
			this.targetClass = targetClass;
			this.targetMethod = targetMethod;
			this.targetSignature = targetSignature;
		}

		public synchronized void add(InstrumentationRule rule) {
			InstrumentationRule[] nrules = new InstrumentationRule[rules == null ? 1 : rules.size() + 1];
			for(int i = 0; i < nrules.length - 1; ++i) {
				nrules[i] = rules.get(i);
			}
			nrules[nrules.length - 1] = rule;
			rules = Arrays.asList(nrules);
		}

		public boolean matches(CutPoint cutPoint) {
			return cutPoint.evaluateCallSite(hostClass, hostMethod, methodSignature, targetClass, targetMethod, targetSignature);
		}
	}	
}
