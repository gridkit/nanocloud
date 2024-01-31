package org.gridkit.nanocloud;

/**
 * A sub interface of {@link ViNode} for node control and life cycle operations.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ViNodeControl {

    public default <X> X x(ViNodeExtender<X> extender) { return extender.wrap(this);};

    public String getProp(String propName);

    public Object getPragma(String pragmaName);

    /**
     * Same as sending empty runnable to node. Usefully to force node initialization.
     */
    public void touch();

    /**
     * Ungracefully terminates remote process (or thread group in embeded node). Unlike {@link #shutdown()} no shutdown hooks will be executed in remote VM.
     * This method may be useful for fault tolerance testing.
     */
    public void kill();

    /**
     * Gracefully terminates remote process (or thread group in embeded node).
     */
    public void shutdown();
}
