package org.gridkit.zerormi;

import org.gridkit.zerormi.RmiChannel2.RmiChannel2Superviser;

public class TestSuperviser implements Superviser, RmiChannel2Superviser {

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
	
	@Override
	public void onFatalError(RmiChannel2 channel, Throwable e) {
		onFatalError(SuperviserEvent.newUnexpectedError(channel, e));
	}

	@Override
	public void onFatalError(RmiChannel2 channel, String message) {
		onFatalError(SuperviserEvent.newUnexpectedError(channel, message));
	}

	@Override
	public void onDestroyFinished(RmiChannel2 channel) {
		onTermination(SuperviserEvent.newClosedEvent(channel));
	}

	@Override
	public void onDestroyInitiated(RmiChannel2 channel) {
		// ignore
	}
}
