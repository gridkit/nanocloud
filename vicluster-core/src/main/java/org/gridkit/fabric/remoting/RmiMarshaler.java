package org.gridkit.fabric.remoting;

import java.io.IOException;

public interface RmiMarshaler {

	public Object writeReplace(Object obj) throws IOException;
	
	public Object readResolve(Object obj) throws IOException;
	
}
