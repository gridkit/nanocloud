package org.gridkit.nanocloud;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 * @param <X>
 */
public interface ViNodeExtender<X> {

    public X wrap(ViNodeControl node);

}
