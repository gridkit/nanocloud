package org.gridkit.nanocloud.interceptor;

import java.io.Serializable;

import org.gridkit.nanocloud.interceptor.ViHookBuilder.ParamMatcher;

class LiteralMatcher implements ParamMatcher, Serializable {

	private static final long serialVersionUID = 20130621L;
	
	private final Object value;
	
	public LiteralMatcher(Object value) {
		this.value = value;
	}

	@Override
	public boolean matches(Object param) {
		return (value == null && param == null) || (value != null && value.equals(param));
	}
}
