package org.gridkit.lab.interceptor.test;

import java.util.concurrent.Callable;

public class SimpleClass_CRT implements Callable<String> {

	@Override
	public String call() throws Exception {		
		return "Hello world!".toUpperCase();
	}	
}
