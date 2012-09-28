package org.gridkit.vicluster.spi;

import java.lang.reflect.Method;

abstract class AbstractViCloudExtention<V> implements ViCloudExtention<V> {

	private final Class<V> facade;
	private final Class<?> wrapperClass;
	private final Class<?>[] hidenFacades;
	
	protected AbstractViCloudExtention(Class<V> facade, Class<?> wrapperClass, Class<?>... hidenFacades) {
		this.facade = facade;
		this.wrapperClass = wrapperClass;
		this.hidenFacades = hidenFacades;
	}

	@Override
	public Class<V> getFacadeInterface() {
		return facade;
	}

	@Override
	public Class<?>[] getHidenInterfaces() {
		return hidenFacades;
	}

	@Override
	public DeferingMode deferingModeForMethod(Method m) {
		return NodeSpiHelper.getMethodModeAnnotation(wrapperClass, m).deferNode();
	}

	@Override
	public GroupCallMode groupModeForMethod(Method m) {
		return NodeSpiHelper.getMethodModeAnnotation(wrapperClass, m).groupCallNode();
	}
}
