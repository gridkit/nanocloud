package org.gridkit.nanocloud.jmx;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

public class MBeanForwarder {
	
	private MBeanServerConnection source;
	private QueryExp filter;
	private MBeanRegistrator target;
	private Executor executor;
	private Map<ObjectName, DynamicMBeanProxy> beans = new HashMap<ObjectName, DynamicMBeanProxy>();
	private boolean listenerInstalled;
	private boolean closed;
	
	private NotificationListener listener = new NotificationListener() {
		@Override
		public void handleNotification(Notification n, Object handback) {
			System.out.println("JMX event: " + n);
			if (closed) {
				return;
			}
			if (n instanceof MBeanServerNotification) {
				MBeanServerNotification event = (MBeanServerNotification) n;
				try {
					if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(n.getType())) {
						final ObjectName name = event.getMBeanName();
						if (filter == null || filter.apply(name)) {
							executor.execute(new Runnable() {
								@Override
								public void run() {
									addBean(name);
								}
							});
						}
					}
					else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(n.getType())) {
						final ObjectName name = event.getMBeanName();
						if (filter == null || filter.apply(name)) {
							executor.execute(new Runnable() {
								@Override
								public void run() {
									removeBean(name);
								}
							});
						}
					}
				} catch (Exception e) {
					// TODO logging
				}			
			}
		}
	};
	
	public MBeanForwarder(MBeanServerConnection mconn, QueryExp filter, MBeanRegistrator registrator, Executor executor) {
		this.source = mconn;
		this.filter = filter;
		this.target = registrator;
		this.executor = executor;
		registerBeanListener();
		refresh();
	}
	
	private synchronized void registerBeanListener() {
		try {
			ObjectName n = new ObjectName("JMImplementation:type=MBeanServerDelegate");
			source.addNotificationListener(n, listener, null, null);
			listenerInstalled = true;
		} catch (MalformedObjectNameException e) {
			// ignore
		} catch (InstanceNotFoundException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
	}

	public boolean isListening() {
		return listenerInstalled;
	}
	
	public void refresh() {
		if (closed) {
			throw new IllegalStateException("Forwarder is closed");
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				syncBeanNames();				
			}
		});
	}
	
	protected synchronized void syncBeanNames() {
		if (closed) {
			return;
		}
		Set<ObjectName> bl;
		try {
			bl = source.queryNames(null, filter);
		} catch (IOException e) {
			// TODO logging
			return;
		}
		Set<ObjectName> missing = new HashSet<ObjectName>(beans.keySet());
		for(ObjectName b: bl) {
			if (!missing.remove(b)) {
				addBean(b);
			}
		}
		for(ObjectName b: missing) {
			removeBean(b);
		}		
	}
	
	private synchronized void addBean(ObjectName b) {
		if (closed) {
			return;
		}
		try {
			DynamicMBeanProxy proxy = new DynamicMBeanProxy(source, b);
			beans.put(b, proxy);
			target.registerMBean(b, proxy);
		}
		catch(Exception e) {
			// TODO logging
		}
	}

	private synchronized void removeBean(ObjectName b) {
		if (closed) {
			return;
		}
		try {
			beans.remove(b);
			target.unregisterMBean(b);
		}
		catch(Exception e) {
			// TODO logging
		}
	}

	public synchronized void close() {
		if (!closed) {
			closed = true;
			if (listenerInstalled) {
				try {
					ObjectName n = new ObjectName("JMImplementation:type=MBeanServerDelegate");
					source.removeNotificationListener(n, listener);
				} catch (MalformedObjectNameException e) {
					// do nothing
				} catch (InstanceNotFoundException e) {
					// do nothing
				} catch (ListenerNotFoundException e) {
					// do nothing
				} catch (IOException e) {
					// do nothing
				}
			}
			for(ObjectName on: beans.keySet()) {
				try {
					target.unregisterMBean(on);
				} catch (MBeanRegistrationException e) {
					// do nothing
				} catch (InstanceNotFoundException e) {
					// do nothing
				}
			}
			beans.clear();
		}
	}
}
