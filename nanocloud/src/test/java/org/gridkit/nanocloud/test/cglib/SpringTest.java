package org.gridkit.nanocloud.test.cglib;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringTest {

	@Test
	public void create_context() {
		createContext();
	}
	
	@Test
	public void create_context_in_isolation() {
		ViNode node = CloudFactory.createCloud().node("test");
		node.x(VX.ISOLATE).setIsolateNodeType();
		
		node.exec(new Runnable() {
			
			@Override
			public void run() {
				createContext();				
			}
		});
	}
	
	@SuppressWarnings("resource")
	public static void createContext() {
		
		AnnotationConfigApplicationContext acac = new AnnotationConfigApplicationContext(MyConfigA.class, MyConfigB.class);
		
		acac.getBean(HelloManager.class).greet();
		
		for(String bn: acac.getBeanDefinitionNames()) {
			BeanDefinition bdef = acac.getBeanDefinition(bn);
			if (bdef instanceof AbstractBeanDefinition) {
				AbstractBeanDefinition abdef = (AbstractBeanDefinition) bdef;
				try {
					abdef.resolveBeanClass(Thread.currentThread().getContextClassLoader());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}	
}
