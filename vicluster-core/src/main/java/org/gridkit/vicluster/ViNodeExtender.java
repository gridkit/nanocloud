package org.gridkit.vicluster;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 * @param <X>
 */
public interface ViNodeExtender<X> {

	public X wrap(ViNode node);

}
