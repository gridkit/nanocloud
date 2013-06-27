package org.gridkit.nanocloud.interceptor;

import java.io.Serializable;
import java.util.Arrays;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.nanocloud.interceptor.ViHookBuilder.ParamMatcher;

class ParamBasedCallFilter implements CallFilter, Serializable {

	private static final long serialVersionUID = 20130621L;
	
	private ParamMatcher[] matchers;
	
	public boolean matches(Interception interception) {
		if (matchers != null) {
			Object[] params = interception.getCallParameters();
			if (matchers.length > params.length) {
				return false;
			}
			for(int i = 0; i != matchers.length; ++i) {
				if (matchers[i] != null) {
					if (!matchers[i].matches(params[i])) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public void addParamMatcher(int n, ParamMatcher matcher) {
		if (matchers == null) {
			matchers = new ParamMatcher[n + 1];
		}
		else if (matchers.length <= n) {
			matchers = Arrays.copyOf(matchers, n + 1);
		}
		matchers[n] = matcher;
	}
}
