package org.gridkit.zerormi.io;

public interface Pin {

	public boolean isBound();

	public boolean isActive();
	
	public interface InputPin<T> extends Pin {
		
	}

	public interface OutputPin<T> extends Pin {
		
	}
	
}
