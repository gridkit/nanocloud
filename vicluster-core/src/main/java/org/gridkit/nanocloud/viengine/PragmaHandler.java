package org.gridkit.nanocloud.viengine;

import java.util.Map;

public interface PragmaHandler {

    public void init(PragmaWriter conext);
    
    public Object query(PragmaWriter context, String key);

    public void apply(PragmaWriter context, Map<String, Object> values);
    
}
