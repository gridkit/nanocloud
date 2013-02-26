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
