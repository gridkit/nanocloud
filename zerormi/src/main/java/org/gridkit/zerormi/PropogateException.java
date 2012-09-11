package org.gridkit.zerormi;

import org.gridkit.util.concurrent.Box;


class PropogateException implements Box<Object> {

	private Box<?> targetBox;
	
	public PropogateException(Box<?> targetBox) {
		this.targetBox = targetBox;
	}

	@Override
	public void setData(Object data) {
		// ignore
	}

	@Override
	public void setError(Throwable e) {
		targetBox.setError(e);		
	}
}
