package org.gridkit.nanocloud.test.cglib;

public class HelloManager {

	private final HelloService service;
	
	public HelloManager(HelloService service) {
		this.service = service;
	}

	public void greet() {
		service.sayHello();
	}	
}
