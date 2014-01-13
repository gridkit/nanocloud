/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.nanocloud.interceptor;

import java.io.Serializable;
import java.util.Arrays;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.nanocloud.interceptor.Intercept.ParamMatcher;

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
