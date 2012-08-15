package org.gridkit.lab.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class is used by synthetic byte hooks.
 * @deprecated It is not meant for direct use. 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@Deprecated
public class ReflectionMethodCallSiteHookContext implements Interception {

	private Class<?> hostClass;
	private String stubMethod;
	private Class<?> targetClass;
	private String targetMethod;
	private String targetMethodSignature;
	private Method method;
	private Object[] parameters;
	private Object result;
	private Throwable exception;
	private boolean resultReady;
	
	@Override
	public HookType getHookType() {
		return HookType.METHOD_CALL_SITE;
	}

	@Override
	public Class<?> getHostClass() {
		return hostClass;
	}

	@Override
	public Object getReflectionObject() {
		if (method == null) {
			String[] pt = getParamTypes(targetMethodSignature);
			for(Method m : targetClass.getDeclaredMethods()) {
				if (m.getParameterTypes().length == pt.length) {
					boolean match = true;
					for(int i = 0; i != pt.length; ++i) {
						if (!pt[i].equals(m.getParameterTypes()[i].getName().replace('.', '/'))) {
							match = false;
							break;
						}
					}
					if (match) {
						method = m;
						break;
					}
				}
			}
		}
		return method;
	}

	private static String[] getParamTypes(String signature) {
		List<String> result = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		int c = signature.lastIndexOf(')');
		String types = signature.substring(1, c);
		boolean longName = false;
		for(int i = 0; i != types.length(); ++i) {
			char x  = types.charAt(i);
			if ('[' == x) {
				sb.append(x);
			}
			else if (';' == x) {
				sb.append(x);
				result.add(toType(sb.toString()));
				sb.setLength(0);
				longName = false;
			}
			else if ('L' == x) {
				sb.append(x);
				longName = true;
			}
			else if (longName){
				sb.append(x);
			}
			else {
				sb.append(x);
				result.add(toType(sb.toString()));
				sb.setLength(0);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	
	private static String toType(String spec) {
		if (spec.startsWith("L")) {
			return spec.substring(1, spec.length() - 1);
		}
		else if ("Z".equals(spec)) {
			return "boolean";
		}
		else if ("B".equals(spec)) {
			return "byte";
		}
		else if ("S".equals(spec)) {
			return "short";
		}
		else if ("C".equals(spec)) {
			return "char";
		}
		else if ("I".equals(spec)) {
			return "int";
		}
		else if ("J".equals(spec)) {
			return "long";
		}
		else if ("F".equals(spec)) {
			return "float";
		}
		else if ("D".equals(spec)) {
			return "double";
		}
		else {
			return spec;
		}
	}

	@Override
	public Object[] getArguments() {
		return parameters;
	}

	@Override
	public Object call() throws ExecutionException {
		
	}

	public boolean isResultReady() {
		return resultReady;	
	}
	
	public Object getResult() {
		return result;
	}
	
	public Throwable getError() {
		return exception;
	}
	
	@Override
	public void setResult(Object r) {
		result = r;
		exception = null;
		resultReady = true;
	}

	@Override
	public void setError(Throwable e) {
		result = null;
		exception = e;
		resultReady = true;
	}
	
	public void setHostClass(Class<?> hostClass) {
		this.hostClass = hostClass;
	}
	
	public void setStubMethod(String stub) {
		this.stubMethod = stub;
	}
	
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public void setTargetMethod(String method) {
		this.targetMethod = method;
	}
	
	public void setTargetMethodSignature(String signature) {
		this.targetMethodSignature = signature;
	}
	
	public void setParameters(Object[] params) {
		this.parameters = params;
	}
}
