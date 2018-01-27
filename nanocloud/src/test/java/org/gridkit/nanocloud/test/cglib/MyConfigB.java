package org.gridkit.nanocloud.test.cglib;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class MyConfigB {

	@Bean @Autowired
	public HelloManager createHelloManager(HelloService service) {
		return new HelloManager(service);
	}	
	
	@Bean
	public HelloMsg message() {
		return new HelloMsg();
	}
}
