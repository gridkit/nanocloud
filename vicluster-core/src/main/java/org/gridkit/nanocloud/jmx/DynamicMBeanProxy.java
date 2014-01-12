package org.gridkit.nanocloud.jmx;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class DynamicMBeanProxy implements DynamicMBean {

	private final MBeanServerConnection connection;
	private final ObjectName bean;
	
	public DynamicMBeanProxy(MBeanServerConnection connection, ObjectName bean) {
		this.connection = connection;
		this.bean = bean;
	}
	
	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		try {
			return connection.getAttribute(bean, attribute);
		} catch (Exception e) {
			throwUncheked(e);
			throw new Error("Unreachable");
		}
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		try {
			connection.setAttribute(bean, attribute);
		} catch (Exception e) {
			throwUncheked(e);
			throw new Error("Unreachable");
		}
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		try {
			return connection.getAttributes(bean, attributes);
		} catch (Exception e) {
			throwUncheked(e);
			throw new Error("Unreachable");
		}
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		try {
			return connection.setAttributes(bean, attributes);
		} catch (Exception e) {
			throwUncheked(e);
			throw new Error("Unreachable");
		}
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		try {
			return connection.invoke(bean, actionName, params, signature);
		} catch (Exception e) {
			throwUncheked(e);
			throw new Error("Unreachable");
		}
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		try {
			return connection.getMBeanInfo(bean);
		} catch (Exception e) {
			throwUncheked(e);
			throw new Error("Unreachable");
		}
	}
	
    private static void throwUncheked(Throwable e) {
        DynamicMBeanProxy.<RuntimeException>throwAny(e);
    }
   
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }
}
