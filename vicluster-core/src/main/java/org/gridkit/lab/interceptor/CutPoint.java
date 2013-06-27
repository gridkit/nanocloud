package org.gridkit.lab.interceptor;

import java.io.Serializable;

public interface CutPoint extends Serializable {

	public boolean evaluateCallSite(String hostClass, String hostMethod, String methodSignature, String targetClass, String targetMethod, String targetSignature);
	
}
