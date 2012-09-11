package org.gridkit.zerormi;

public class TestSuperviser implements ComponentSuperviser {

	@Override
	public void onWarning(SuperviserEvent event) {
		System.err.println(event);
	}

	@Override
	public void onTermination(SuperviserEvent event) {
		System.err.println(event);
	}

	@Override
	public void onFatalError(SuperviserEvent event) {
		System.err.println(event);
	}
}
