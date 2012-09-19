package org.gridkit.lab.interceptor.isolate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;

public class InterceptionAnchor {

	private static int callSiteCounter;
	
	private static List<Interceptor> probes = new ArrayList<Interceptor>();
	
	private static volatile int[][] hooks = new int[16][];
	
	public synchronized static int registerInterceptor(Interceptor probe) {
		int n = probes.size();
		probes.add(probe);
		return n;
	}
	
	public synchronized static int addHook(int[] probes) {
		int n = callSiteCounter++;
		if (n > hooks.length) {
			int[][] nhooks = Arrays.copyOf(hooks, hooks.length + 64);
			hooks = nhooks;
		}
		hooks[n] = probes;
		return n;
	}
	
	public static void dispatch(int hookId, Interception invocation) {
		for (int probe: hooks[hookId]) {
			probes.get(probe).handle(invocation);
		}
	}	
}
