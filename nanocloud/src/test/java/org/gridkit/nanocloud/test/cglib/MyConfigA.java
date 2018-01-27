package org.gridkit.nanocloud.test.cglib;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyConfigA {

	@Autowired(required = true)
	HelloMsg message;
	
	@Bean
	public HelloService createHelloService() {
		return new HelloService() {
			
			@Override
			public void sayHello() {
				System.out.println(message.toString());				
			}
		};
	}
	
	
}
