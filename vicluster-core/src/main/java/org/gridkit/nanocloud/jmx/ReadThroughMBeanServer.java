package org.gridkit.nanocloud.jmx;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import javax.management.MBeanServer;
import javax.management.loading.ClassLoaderRepository;

public class ReadThroughMBeanServer implements MBeanServer {
	
	private MBeanServerConnection backRegistry;
	private MBeanServer frontRegistry;
	
	public ReadThroughMBeanServer(MBeanServerConnection backRegistry, MBeanServer frontRegistry) {
		this.backRegistry = backRegistry;
		this.frontRegistry = frontRegistry;
	}

	private MBeanServerConnection resolve(ObjectName name) {
		if (frontRegistry.isRegistered(name)) {
			return frontRegistry;
		}
		else {
			return backRegistry;
		}
	}
	
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        if (isRegistered(name)) {
        	throw new InstanceAlreadyExistsException(name.toString());
        }
    	return frontRegistry.createMBean(className, name);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        if (isRegistered(name)) {
            throw new InstanceAlreadyExistsException(name.toString());
        }
        return frontRegistry.createMBean(className, name, loaderName);
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        if (isRegistered(name)) {
            throw new InstanceAlreadyExistsException(name.toString());
        }
        return frontRegistry.createMBean(className, name, params, signature);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        if (isRegistered(name)) {
            throw new InstanceAlreadyExistsException(name.toString());
        }
        return frontRegistry.createMBean(className, name, loaderName, params, signature);
    }

    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        if (isRegistered(name)) {
            throw new InstanceAlreadyExistsException(name.toString());
        }
        return frontRegistry.registerMBean(object, name);
    }

    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        frontRegistry.unregisterMBean(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        try {
            return resolve(name).getObjectInstance(name);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        try {
            Map<ObjectName, ObjectInstance> buf = new HashMap<ObjectName, ObjectInstance>();
            for(ObjectInstance oi: backRegistry.queryMBeans(name, query)) {
            	buf.put(oi.getObjectName(), oi);
            }
            for(ObjectInstance oi: frontRegistry.queryMBeans(name, query)) {
                buf.put(oi.getObjectName(), oi);
            }
            return new HashSet<ObjectInstance>(buf.values());
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        try {
            Set<ObjectName> buf = new HashSet<ObjectName>();
            buf.addAll(backRegistry.queryNames(name, query));
            buf.addAll(frontRegistry.queryNames(name, query));
            return buf;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRegistered(ObjectName name) {
        try {
            return frontRegistry.isRegistered(name) || backRegistry.isRegistered(name);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

    public Integer getMBeanCount() {
        return queryNames(null, null).size();
    }

    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        try {
            return resolve(name).getAttribute(name, attribute);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        try {
            return resolve(name).getAttributes(name, attributes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        try {
            resolve(name).setAttribute(name, attribute);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        try {
            return resolve(name).setAttributes(name, attributes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        try {
            return resolve(name).invoke(name, operationName, params, signature);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDefaultDomain() {
        return frontRegistry.getDefaultDomain();
    }

    public String[] getDomains() {
    	try {
            Set<String> domains = new LinkedHashSet<String>();
            domains.addAll(Arrays.asList(frontRegistry.getDomains()));
            domains.addAll(Arrays.asList(backRegistry.getDomains()));
            
            return domains.toArray(new String[domains.size()]);
        } catch (IOException e) {
        	throw new RuntimeException();
        }
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        try {
            resolve(name).addNotificationListener(name, listener, filter, handback);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        try {
            resolve(name).addNotificationListener(name, listener, filter, handback);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            resolve(name).removeNotificationListener(name, listener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            resolve(name).removeNotificationListener(name, listener, filter, handback);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            resolve(name).removeNotificationListener(name, listener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            resolve(name).removeNotificationListener(name, listener, filter, handback);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
		try {
		    return resolve(name).getMBeanInfo(name);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        try {
            return resolve(name).isInstanceOf(name, className);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return frontRegistry.instantiate(className);
    }

    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return frontRegistry.instantiate(className, loaderName);
    }

    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        return frontRegistry.instantiate(className, params, signature);
    }

    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return frontRegistry.instantiate(className, loaderName, params, signature);
    }

    @SuppressWarnings("deprecation")
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
        return frontRegistry.deserialize(name, data);
    }

    @SuppressWarnings("deprecation")
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        return frontRegistry.deserialize(className, data);
    }

    @SuppressWarnings("deprecation")
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
        return frontRegistry.deserialize(className, loaderName, data);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        return frontRegistry.getClassLoaderFor(mbeanName);
    }

    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        return frontRegistry.getClassLoader(loaderName);
    }

    public ClassLoaderRepository getClassLoaderRepository() {
        return frontRegistry.getClassLoaderRepository();
    }
}
