package org.gridkit.nanocloud.viengine;

import java.util.Map;

public interface PragmaHandler {

    /**
     * Forbid modification after boot phase. Noop otherwise.
     */
    public static PragmaHandler PASSIVE = new PassivePragmaHandler();

    /**
     * Called at the beginning of node bootstrap.
     */
    public void configure(PragmaWriter context);

    /**
     * Called when node is ready.
     */
    public void init(PragmaWriter conext);

    public Object query(PragmaWriter context, String key);

    /**
     * Initial configuration of values.
     */
    public void setup(PragmaWriter context, Map<String, Object> config);

    /**
     * Runtime update of pragma properties.
     */
    public void apply(PragmaWriter context, Map<String, Object> values);

}
