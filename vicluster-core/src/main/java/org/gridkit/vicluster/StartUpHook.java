package org.gridkit.vicluster;

/**
 * This could be used for hooks which cannot be run on initialized node.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface StartUpHook extends Runnable {
	
	public void onInit();

}
