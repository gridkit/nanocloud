package org.gridkit.nanocloud.interceptor;

import java.io.Serializable;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;

class FilterableInterceptor implements Interceptor, Serializable {

	private static final long serialVersionUID = 20130621L;

	private CallFilter filter;
	private Interceptor nested;

	public FilterableInterceptor(CallFilter filter, Interceptor nested) {
		this.filter = filter;
		this.nested = nested;
	}

	@Override
	public void handle(Interception call) {
		if (filter.matches(call)) {
			nested.handle(call);
		}
	}
}
