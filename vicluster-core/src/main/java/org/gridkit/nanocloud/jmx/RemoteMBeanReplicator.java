package org.gridkit.nanocloud.jmx;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

public class RemoteMBeanReplicator implements Serializable {

	private static final long serialVersionUID = 20140112L;
	
	private RemoteMBeanRegistrator target;
	private transient List<MBeanForwarder> forwarders;
	private transient BatchingRegistrator registrator;
	
	public RemoteMBeanReplicator(MBeanRegistrator registry) {
		this.target = new MBeanRegistratorSkeleton(registry);
	}

	public synchronized void export(QueryExp filter, MBeanServerConnection conn) {
		if (registrator == null) {
			registrator = new BatchingRegistrator(target);
		}
		if (forwarders == null) {
			forwarders = new ArrayList<MBeanForwarder>();
		}
		MBeanForwarder forwarder = new MBeanForwarder(conn, filter, registrator, registrator.getThreadPool());
		forwarders.add(forwarder);
	}

	public synchronized void stop() {
		for(MBeanForwarder f: forwarders) {
			f.close();
		}
		forwarders.clear();
	}
	
	private class BatchingRegistrator implements MBeanRegistrator, Runnable {

		private RemoteMBeanRegistrator target;
		private ExecutorService executor;
		private Map<ObjectName, DynamicMBean> buffer = new HashMap<ObjectName, DynamicMBean>();
		private boolean pending;

		public BatchingRegistrator(RemoteMBeanRegistrator target) {
			this.target = target;
			this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setDaemon(true);
					t.setName("RemoteMBeanReplicator");
					return t;
				}
			});
		}

		public Executor getThreadPool() {
			return executor;
		}

		@Override
		public synchronized void registerMBean(ObjectName name, Object bean) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
			buffer.put(name, wrap(bean));
			signal();
		}

		private DynamicMBean wrap(Object bean) {
			DynamicMBean dyn = ((DynamicMBean)bean);
			return new MetaCachedDynamicMBean(new DynamicMBeanSkeleton(dyn));
		}

		@Override
		public synchronized void unregisterMBean(ObjectName name) throws MBeanRegistrationException, InstanceNotFoundException {
			if (buffer.containsKey(name)) {
				buffer.remove(name);
			}
			else {
				buffer.put(name, null);
			}
			signal();
		}

		private synchronized void signal() {
			if (!pending) {
				pending = true;
				executor.submit(this);
			}
		}

		@Override
		public synchronized void run() {
			target.updateMBeans(buffer);
			buffer.clear();
			pending = false;
			
		}
	}
	
	private interface RemoteMBeanRegistrator extends Remote {
	
		public void updateMBeans(Map<ObjectName, DynamicMBean> mbeans);
		
	}
	
	private class MBeanRegistratorSkeleton implements RemoteMBeanRegistrator {
	
		private MBeanRegistrator target;

		public MBeanRegistratorSkeleton(MBeanRegistrator target) {
			this.target = target;
		}

		@Override
		public void updateMBeans(Map<ObjectName, DynamicMBean> mbeans) {
			for(ObjectName on: mbeans.keySet()) {
				try {
					DynamicMBean mbean = mbeans.get(on);
					if (mbean == null) {
						target.unregisterMBean(on);
					}
					else {
						target.registerMBean(on, mbean);
					}
				} catch (Exception e) {
					// ignore
				}
			}
			
		}
	}
	
	private interface RemoteDynamicMBean extends DynamicMBean, Remote {
		
	}
	
	private class DynamicMBeanSkeleton implements RemoteDynamicMBean {
		
		private DynamicMBean targetBean;

		public DynamicMBeanSkeleton(DynamicMBean targetBean) {
			this.targetBean = targetBean;
		}

		public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
			return targetBean.getAttribute(attribute);
		}

		public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
			targetBean.setAttribute(attribute);
		}

		public AttributeList getAttributes(String[] attributes) {
			return targetBean.getAttributes(attributes);
		}

		public AttributeList setAttributes(AttributeList attributes) {
			return targetBean.setAttributes(attributes);
		}

		public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
			return targetBean.invoke(actionName, params, signature);
		}

		public MBeanInfo getMBeanInfo() {
			return targetBean.getMBeanInfo();
		}		
	}
	
	private class MetaCachedDynamicMBean implements DynamicMBean, Serializable {

		private static final long serialVersionUID = 20140112L;
		
		private DynamicMBean mbean;
		private MBeanInfo mbeanInfo;

		public MetaCachedDynamicMBean(DynamicMBean mbean) {
			this.mbean = mbean;
			this.mbeanInfo = mbean.getMBeanInfo();
		}
		
		public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
			return mbean.getAttribute(attribute);
		}

		public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
			mbean.setAttribute(attribute);
		}

		public AttributeList getAttributes(String[] attributes) {
			return mbean.getAttributes(attributes);
		}

		public AttributeList setAttributes(AttributeList attributes) {
			return mbean.setAttributes(attributes);
		}

		public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
			return mbean.invoke(actionName, params, signature);
		}

		public MBeanInfo getMBeanInfo() {
			return mbeanInfo;
		}
	}
}
