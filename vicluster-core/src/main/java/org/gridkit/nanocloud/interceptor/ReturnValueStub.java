package org.gridkit.nanocloud.interceptor;

import java.io.Serializable;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;

class ReturnValueStub implements Interceptor, Serializable {

	private static final long serialVersionUID = 20130621L;
	
	private Object value;

	public ReturnValueStub(Object value) {
		this.value = value;
	}

	@Override
	public void handle(Interception call) {
		call.setResult(value);
	}
}
