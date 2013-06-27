package org.gridkit.nanocloud.interceptor;

import org.gridkit.lab.interceptor.Interception;

public interface CallFilter {

	public boolean matches(Interception call);
	
}
