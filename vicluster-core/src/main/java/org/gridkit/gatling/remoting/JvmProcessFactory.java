package org.gridkit.gatling.remoting;

import java.io.IOException;

public interface JvmProcessFactory {
	
	public ControlledProcess createProcess(JvmConfig jvmArgs) throws IOException;

}
