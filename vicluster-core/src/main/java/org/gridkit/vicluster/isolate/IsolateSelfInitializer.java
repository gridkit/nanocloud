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
package org.gridkit.vicluster.isolate;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class IsolateSelfInitializer implements Runnable, Serializable {

	private final Map<String, String> props;
	
	public IsolateSelfInitializer(Map<String, String> props) {
		this.props = props;
	}

	@Override
	public void run() {
		Isolate isolate = Isolate.currentIsolate();
		if (isolate == null) {
			throw new RuntimeException("No isolate found");
		}
		apply(isolate);
	}

	public void apply(Isolate isolate) {
		List<Object> cpRules = new ArrayList<Object>();
		for(String key: props.keySet()) {
			try {
				String val;
				if (null != (val = matchProp(key, IsolateProps.ISOLATE_PACKAGE))) {
					isolate.addPackageRule(cpRules, val, true);
				}
				else if (null != (val = matchProp(key, IsolateProps.SHARE_PACKAGE))) {
					isolate.addPackageRule(cpRules, val, false);
				}
				else if (null != (val = matchProp(key, IsolateProps.ISOLATE_CLASS))) {
					isolate.addClassRule(cpRules, val, true);
				}
				else if (null != (val = matchProp(key, IsolateProps.SHARE_CLASS))) {
					isolate.addClassRule(cpRules, val, false);
				}
				else if (null != (val = matchProp(key, IsolateProps.ISOLATE_URL))) {
					isolate.addUrlRule(cpRules, new URL(val), true);
				}
				else if (null != (val = matchProp(key, IsolateProps.SHARE_URL))) {
					isolate.addUrlRule(cpRules, new URL(val), false);
				}
				else if (null != (val = matchProp(key, IsolateProps.CP_INCLUDE))) {
					isolate.addToClasspath(new URL(val));
				}
				else if (null != (val = matchProp(key, IsolateProps.CP_EXCLUDE))) {
					isolate.removeFromClasspath(new URL(val));
				}
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}			
		}		
		isolate.applyRules(cpRules);
	}
	
	private String matchProp(String propName, String optionName) {
		if (propName.startsWith(optionName)) {
			String val = propName.substring(optionName.length());
			return val;
		}
		else {
			return null;
		}
	}
}
