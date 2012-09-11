package org.gridkit.lab.interceptor;


/**
 * Handler of execution interception event.
 * Class implementing this interface may witness or override behavior of intercepted call. 
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Interceptor {
	
	public void handle(Interception hook);

}
