package org.gridkit.nanocloud.jmx;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public interface MBeanRegistrator {
	
	public void registerMBean(ObjectName name, Object bean) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException;

	public void unregisterMBean(ObjectName name) throws MBeanRegistrationException, InstanceNotFoundException;

	public static class MBeanServerRegistrator implements MBeanRegistrator {
		
		private MBeanServer server;

		public MBeanServerRegistrator(MBeanServer server) {
			this.server = server;
		}

		@Override
		public void registerMBean(ObjectName name, Object bean) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
			server.registerMBean(bean, name);
		}

		@Override
		public void unregisterMBean(ObjectName name) throws MBeanRegistrationException, InstanceNotFoundException {
			server.unregisterMBean(name);
		}
	}
	
	public static abstract class MBeanRenamer implements MBeanRegistrator {
		
		private MBeanRegistrator delegate;

		public MBeanRenamer(MBeanRegistrator delegate) {
			this.delegate = delegate;
		}

		@Override
		public void registerMBean(ObjectName name, Object bean) throws InstanceAlreadyExistsException,	MBeanRegistrationException, NotCompliantMBeanException {
			delegate.registerMBean(rename(name), bean);
			
		}

		@Override
		public void unregisterMBean(ObjectName name) throws MBeanRegistrationException, InstanceNotFoundException {
			delegate.unregisterMBean(rename(name));
		}

		protected abstract ObjectName rename(ObjectName name);
	}

	public static class MBeanDomainSwitcher extends MBeanRenamer {
		
		private String newDomain;

		public MBeanDomainSwitcher(MBeanRegistrator delegate, String newDomain) {
			super(delegate);
			this.newDomain = newDomain;
		}

		protected ObjectName rename(ObjectName name) {
			String n = name.toString();
			n = n.substring(name.getDomain().length());
			n = newDomain + n;
			try {
				return new ObjectName(n);
			} catch (MalformedObjectNameException e) {
				throw new RuntimeException(e);
			}
		}
	}	
}
