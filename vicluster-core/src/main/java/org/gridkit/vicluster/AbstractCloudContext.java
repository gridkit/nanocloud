package org.gridkit.vicluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AbstractCloudContext implements CloudContext {

	private static final String DUP = new String("DUP");
	
	protected java.util.Map<Class<?>, java.util.Map<ServiceKey<?>, Object>> config = new HashMap<Class<?>, java.util.Map<ServiceKey<?>,Object>>();
	protected List<Runnable> finalizers = new ArrayList<Runnable>();
	
	@Override
	public synchronized <T> T lookup(ServiceKey<T> key) {
		if (config.containsKey(key.getType())) {
			Object s = config.get(key.getType()).get(key);
			if (s == DUP) {
				throw new IllegalArgumentException("Key is ambigous: " + key);
			}
			if (s != null) {
				return key.getType().cast(s);
			}
		}
		return null;
	}

	@Override
	public synchronized <T> T lookup(ServiceKey<T> key, Callable<T> provider) {
		T s = lookup(key);
		if (s == null) {
			try {
				s = provider.call();
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException)e;
				}
				throw new RuntimeException(e);
			}
			add(key, s);
		}
		return s;
	}

	@Override
	public synchronized <T> T lookup(ServiceKey<T> key, ServiceProvider<T> provider) {
		T s = lookup(key);
		if (s == null) {
			s = provider.getService(this);
			add(key, s);
		}
		return s;
	}
	
	@Override
	public synchronized void addFinalizer(Runnable finalizer) {
		finalizers.add(finalizer);
	}

	protected synchronized void runFinalizers() {
		for(Runnable r: finalizers) {
			try {
				r.run();
			}
			catch(Exception e) {
				// TODO logging
			}
		}
	}
	
	protected synchronized void add(ServiceKey<?> key, Object service) {
		for(Class<?> p : key.getClassHierary()) {
			java.util.Map<ServiceKey<?>, Object> entry = config.get(p);
			if (entry == null) {
				config.put(p, entry = new HashMap<CloudContext.ServiceKey<?>, Object>());
			}
			if (entry.containsKey(key)) {
				entry.put(key, DUP);
			}
			else {
				entry.put(key, service);
			}
		}		
	}
}
