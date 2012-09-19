package org.gridkit.vicluster.isolate.interceptor;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;

public class NanoTimeProbe implements Interceptor, Serializable {

	@Override
	public void handle(Interception hook) {
		System.out.println("Adjusting nanoTime()");
		try {
			long nanoTime = (Long) hook.call();
			hook.setResult(nanoTime + 10000000000l);
		} catch (ExecutionException e) {
			// ignore
		}
	}
}
